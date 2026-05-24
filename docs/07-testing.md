# 07 — Unit Testing in KMP

Testing di KMP sedikit beda dari Android murni karena
kita pengen test jalan di **semua platform** (Android,
iOS, Desktop) dari `commonTest`. Konsekuensinya: pilihan
library yang truly multiplatform lebih terbatas.

---

## Mapping Flutter Testing → KMP

| Flutter | KMP |
|---|---|
| `flutter_test` | `kotlin.test` |
| `mockito` / `mocktail` | **Manual Fakes** ⭐ (multiplatform) |
| `bloc_test` | `Turbine` (Flow testing) |
| `expectLater` | `flow.test { awaitItem() }` |
| `when()` | Set property di Fake |
| `verify()` | Check counter property di Fake |
| `tester.pump()` | `runCurrent()` / `advanceUntilIdle()` |

---

## Kenapa Manual Fakes (Bukan MockK)?

| Library | Multiplatform? | Verdict |
|---|---|---|
| **kotlin.test** | ✅ Ya | ✅ Pakai |
| **kotlinx-coroutines-test** | ✅ Ya | ✅ Pakai |
| **Turbine** | ✅ Ya | ✅ Pakai |
| **MockK** | ❌ Mostly JVM-only | ⚠️ Hindari di commonTest |
| **Mockito** | ❌ JVM-only | ❌ Skip |

**MockK** bagus untuk Android-only test (taruh di `androidUnitTest`/`jvmTest`),
tapi untuk `commonTest` yang harus jalan di iOS juga,
**manual Fake** adalah pilihan terbaik.

### Pendekatan Fake

Tulis class manual yang implement interface yang sama:

```kotlin
class FakeBlogsApi : BlogsApi {

    // Setter: atur perilaku dari test
    var blogsResponse: List<BlogDto> = emptyList()
    var shouldThrow: Boolean = false

    // Counter: verifikasi pemanggilan
    var getBlogsCallCount: Int = 0

    override suspend fun getBlogs(): List<BlogDto> {
        getBlogsCallCount++
        if (shouldThrow) throw RuntimeException("Test error")
        return blogsResponse
    }

    // ...
}
```

COMMENT:
- ✅ Type-safe (compiler check signature)
- ✅ Bisa di-control penuh dari test
- ✅ Jalan di semua platform
- ✅ Lebih pedagogis - jelas perilakunya
- ❌ Verbose (tulis sendiri)

---

## Dependency

`gradle/libs.versions.toml`

```toml
[versions]
turbine = "1.1.0"
kotlinx-coroutines = "1.11.0"

[libraries]
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

`shared/build.gradle.kts`

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)
}
```

---

## Folder Structure Test

Mirror folder structure `commonMain`:

```txt
shared/src/commonTest/kotlin/.../features/blogs/
├── fakes/                          ← Fake test doubles
│   ├── FakeBlogsApi.kt
│   └── FakeBlogRepository.kt
├── data/
│   └── repository/
│       └── BlogRepositoryImplTest.kt
├── domain/
│   └── usecases/
│       ├── GetBlogsUseCaseTest.kt
│       └── GetBlogByIdUseCaseTest.kt
└── presentations/
    ├── list/
    │   └── BlogViewModelTest.kt
    └── detail/
        └── BlogDetailViewModelTest.kt
```

COMMENT:
- `fakes/` folder berisi semua Fake (reusable
  antar test)
- Tiap layer punya folder test sendiri

---

# Test per Layer

---

## 1. Fake (Test Doubles)

Fake dibuat **sekali**, dipakai di banyak test.

`commonTest/.../fakes/FakeBlogsApi.kt`

```kotlin
class FakeBlogsApi : BlogsApi {

    // Setter untuk response
    var blogsResponse: List<BlogDto> = emptyList()
    var blogByIdResponse: BlogDto? = null

    // Setter untuk simulasi error
    var shouldThrowOnGetBlogs: Boolean = false
    var shouldThrowOnGetBlogById: Boolean = false
    var thrownException: Exception = RuntimeException("Test exception")

    // Counter untuk verifikasi
    var getBlogsCallCount: Int = 0
    var getBlogByIdCallCount: Int = 0
    var lastRequestedId: Int? = null

    override suspend fun getBlogs(): List<BlogDto> {
        getBlogsCallCount++
        if (shouldThrowOnGetBlogs) throw thrownException
        return blogsResponse
    }

    override suspend fun getBlogById(id: Int): BlogDto {
        getBlogByIdCallCount++
        lastRequestedId = id
        if (shouldThrowOnGetBlogById) throw thrownException
        return blogByIdResponse
            ?: throw IllegalStateException("blogByIdResponse not set")
    }
}
```

COMMENT:
- **Setter properties** ≈ `when()` di Mockito
- **Counter properties** ≈ `verify()` di Mockito
- Mirip pattern Fake di Flutter (manual class)

---

## 2. Repository Test

Test fokus:
- Mapping DTO → Domain
- Error handling

`commonTest/.../data/repository/BlogRepositoryImplTest.kt`

```kotlin
class BlogRepositoryImplTest {

    private lateinit var fakeApi: FakeBlogsApi
    private lateinit var repository: BlogRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeApi = FakeBlogsApi()
        repository = BlogRepositoryImpl(fakeApi)
    }

    @Test
    fun `getBlogs returns Success and maps DTO to Domain`() = runTest {

        // Given
        fakeApi.blogsResponse = listOf(
            BlogDto(userId = 1, id = 1, title = "First", body = "Body"),
        )

        // When
        val result = repository.getBlogs()

        // Then
        assertIs<Result.Success<List<*>>>(result)
        assertEquals(1, result.data.size)
        assertEquals("First", result.data[0].title)
    }

    @Test
    fun `getBlogs returns Error when API throws`() = runTest {

        // Given
        fakeApi.shouldThrowOnGetBlogs = true
        fakeApi.thrownException = RuntimeException("No internet")

        // When
        val result = repository.getBlogs()

        // Then
        assertIs<Result.Error>(result)
        assertIs<Failure.NetworkFailure>(result.failure)
        assertEquals("No internet", result.failure.message)
    }
}
```

COMMENT:
- Pattern **Given-When-Then** = bikin test gampang dibaca
- `assertIs<T>(value)` ≈ type check + smart cast
- `runTest { }` dari `kotlinx-coroutines-test` untuk
  test suspend function

### Apa yang Di-test di Repository?

| ✅ Yes | ❌ No |
|---|---|
| Mapping DTO → Domain | UI logic |
| Error handling (catch exception) | Validasi business rule |
| Convert exception ke `Failure` | Networking (itu Api job) |
| Composing multiple data source | Cara render data |

---

## 3. UseCase Test

UseCase biasanya tipis (cuma delegate ke Repository).
Test-nya simple: pass-through.

`commonTest/.../domain/usecases/GetBlogsUseCaseTest.kt`

```kotlin
class GetBlogsUseCaseTest {

    private lateinit var fakeRepository: FakeBlogRepository
    private lateinit var useCase: GetBlogsUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeBlogRepository()
        useCase = GetBlogsUseCase(fakeRepository)
    }

    @Test
    fun `invoke returns Success from repository`() = runTest {

        // Given
        val expectedBlogs = listOf(
            BlogModel(userId = 1, id = 1, title = "Test", body = "Body"),
        )
        fakeRepository.blogsResult = Result.Success(expectedBlogs)

        // When
        val result = useCase()

        // Then
        assertIs<Result.Success<*>>(result)
        assertEquals(expectedBlogs, result.data)
        assertEquals(1, fakeRepository.getBlogsCallCount)
    }

    @Test
    fun `invoke returns Error from repository`() = runTest {

        // Given
        fakeRepository.blogsResult = Result.Error(
            Failure.NetworkFailure("Connection failed")
        )

        // When
        val result = useCase()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Connection failed", result.failure.message)
    }
}
```

COMMENT:
- UseCase test sebagian besar = verifikasi
  delegation works
- Test akan lebih berarti kalau UseCase punya
  logic (aggregating data, validation, dll)

### UseCase dengan Parameter

```kotlin
@Test
fun `invoke passes id to repository`() = runTest {

    fakeRepository.blogByIdResult = Result.Success(blog)

    useCase(42)

    // Verifikasi id diteruskan
    assertEquals(42, fakeRepository.lastRequestedId)
}
```

---

## 4. ViewModel Test (MVI dengan Turbine)

Ini bagian paling kompleks tapi paling penting.
Test fokus:
- State transitions (Initial → Loading → Success/Failure)
- Effect emissions (Snackbar, Navigation)
- Intent handling

### Setup Main Dispatcher

`viewModelScope` pakai `Dispatchers.Main`. Di test,
kita perlu **override** Main supaya bisa di-control:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class BlogViewModelTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

COMMENT:
- `Dispatchers.setMain()` dari `kotlinx-coroutines-test`
- `StandardTestDispatcher` = manual control, eksplisit
  (vs `UnconfinedTestDispatcher` yang langsung run)
- WAJIB `resetMain()` di `@AfterTest` supaya gak
  bocor ke test lain

### Test Initial Load (init block)

```kotlin
@Test
fun `init triggers LoadBlogs and ends in Success`() = runTest {

    // Given
    val blogs = listOf(
        BlogModel(userId = 1, id = 1, title = "Test", body = "Body")
    )
    fakeRepository.blogsResult = Result.Success(blogs)

    // When - create ViewModel
    // init { onIntent(LoadBlogs) } akan run
    val viewModel = BlogViewModel(useCase)
    runCurrent()  // ← let queued coroutine run

    // Then
    val state = viewModel.state.value
    assertIs<BlogState.Success>(state)
    assertEquals(blogs, state.blogs)
}
```

COMMENT:
- `runCurrent()` = jalankan coroutine yang sudah
  di-queue di scheduler
- Tanpa ini, state masih `Initial` karena coroutine
  belum dijalankan

### Test State Transitions (Turbine)

```kotlin
@Test
fun `state transitions Initial then Loading then Success`() = runTest {

    fakeRepository.blogsResult = Result.Success(blogs)

    val viewModel = BlogViewModel(useCase)

    viewModel.state.test {

        // 1. Initial state (StateFlow always emits current value first)
        assertEquals(BlogState.Initial, awaitItem())

        // 2. Loading
        assertEquals(BlogState.Loading, awaitItem())

        // 3. Success
        val success = awaitItem()
        assertIs<BlogState.Success>(success)
        assertEquals(blogs, success.blogs)

        cancelAndConsumeRemainingEvents()
    }
}
```

COMMENT:
- `flow.test { }` dari Turbine
- `awaitItem()` = tunggu emisi berikutnya
- StateFlow **selalu** emit current value pertama kali
  saat di-subscribe (`Initial` di sini)
- `cancelAndConsumeRemainingEvents()` di akhir
  supaya gak hang menunggu emisi lain

### Test Effect (Channel + Turbine)

```kotlin
@Test
fun `BlogClicked emits NavigateToDetail effect`() = runTest {

    fakeRepository.blogsResult = Result.Success(emptyList())
    val viewModel = BlogViewModel(useCase)
    runCurrent()  // skip init load

    // When + Then
    viewModel.effect.test {

        viewModel.onIntent(BlogIntent.BlogClicked(id = 42))

        val effect = awaitItem()
        assertIs<BlogEffect.NavigateToDetail>(effect)
        assertEquals(42, effect.blogId)
    }
}
```

COMMENT:
- Effect pakai `Channel` (one-shot, gak ada current value)
- Jadi `awaitItem()` langsung wait emisi pertama
- Pattern sama dengan State, beda di nature emission

### Test Refresh dengan Data Lama

```kotlin
@Test
fun `Refresh from Success keeps blogs and sets isRefreshing`() = runTest {

    // Setup initial Success
    fakeRepository.blogsResult = Result.Success(initialBlogs)
    val viewModel = BlogViewModel(useCase)
    runCurrent()

    // Setup refresh response
    fakeRepository.blogsResult = Result.Success(refreshedBlogs)

    viewModel.state.test {

        // 1. Current state (sudah Success)
        val before = awaitItem()
        assertIs<BlogState.Success>(before)

        // 2. Trigger refresh
        viewModel.onIntent(BlogIntent.Refresh)

        // 3. Selama refresh: tetap Success, isRefreshing=true,
        //    data lama tetap tampil
        val refreshing = awaitItem()
        assertIs<BlogState.Success>(refreshing)
        assertTrue(refreshing.isRefreshing)
        assertEquals(initialBlogs, refreshing.blogs)  // ← data lama!

        // 4. Setelah selesai
        val finalState = awaitItem()
        assertIs<BlogState.Success>(finalState)
        assertFalse(finalState.isRefreshing)
        assertEquals(refreshedBlogs, finalState.blogs)
    }
}
```

---

## Cheat Sheet: Test Tool

| Tool | Untuk Apa |
|---|---|
| `runTest { }` | Wrap test yang punya suspend |
| `runCurrent()` | Run coroutine yang udah di-queue |
| `advanceUntilIdle()` | Run semua coroutine sampai habis |
| `advanceTimeBy(ms)` | Skip delay/timer |
| `flow.test { }` | Turbine block untuk observe Flow |
| `awaitItem()` | Tunggu emisi Flow berikutnya |
| `cancelAndConsumeRemainingEvents()` | Cleanup di akhir Turbine block |
| `assertIs<T>(value)` | Type check + smart cast |
| `assertEquals(a, b)` | Equality check |
| `Dispatchers.setMain(...)` | Override Main untuk test |
| `Dispatchers.resetMain()` | Cleanup di `@AfterTest` |

---

## Pattern Given-When-Then

Selalu structure test pakai 3 phase ini:

```kotlin
@Test
fun `xxx`() = runTest {

    // Given - setup state awal
    fakeRepository.blogsResult = Result.Success(blogs)

    // When - lakukan aksi yang di-test
    val result = useCase()

    // Then - verifikasi expectation
    assertIs<Result.Success<*>>(result)
}
```

COMMENT:
- Setiap section dipisah dengan comment yang jelas
- Bikin reviewer langsung paham flow test-nya
- Mirip "Arrange-Act-Assert" di Flutter

---

## 5. Core Layer Test

Selain feature, **core** layer (Result, Failure, Koin modules)
juga perlu di-test. Test ini lebih simple tapi penting untuk
catch regression.

---

### Result Test (Sealed Class Wrapper)

`commonTest/.../core/util/ResultTest.kt`

```kotlin
class ResultTest {

    @Test
    fun `Success holds the data passed to it`() {

        val result: Result<String> = Result.Success("Hello")

        val success = assertIs<Result.Success<String>>(result)
        assertEquals("Hello", success.data)
    }

    @Test
    fun `Error holds the failure passed to it`() {

        val failure = Failure.NetworkFailure("No internet")
        val result: Result<String> = Result.Error(failure)

        val error = assertIs<Result.Error>(result)
        assertEquals(failure, error.failure)
    }

    @Test
    fun `when block exhaustive with smart cast`() {

        val result: Result<Int> = Result.Success(42)

        val output = when (result) {
            is Result.Success -> "Got ${result.data}"  // smart cast
            is Result.Error -> "Error: ${result.failure.message}"
        }

        assertEquals("Got 42", output)
    }

    @Test
    fun `Two Success with same data are equal (data class)`() {

        val a = Result.Success("hello")
        val b = Result.Success("hello")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
```

COMMENT:
- Test data class behavior (equality, hashCode)
- Test pattern matching dengan smart cast
- Test generic type preservation

---

### Failure Test (Sealed Class Hierarchy)

`commonTest/.../core/util/FailureTest.kt`

```kotlin
class FailureTest {

    @Test
    fun `NetworkFailure exposes message via parent Failure property`() {

        // Type sebagai parent Failure
        val failure: Failure = Failure.NetworkFailure("Offline")

        // .message accessible via parent `open val`
        assertEquals("Offline", failure.message)
    }

    @Test
    fun `when block distinguishes subtypes`() {

        val failure: Failure = Failure.NetworkFailure("offline")

        val type = when (failure) {
            is Failure.NetworkFailure -> "network"
            is Failure.UnknownFailure -> "unknown"
        }

        assertEquals("network", type)
    }

    @Test
    fun `NetworkFailure and UnknownFailure with same message are NOT equal`() {

        val network: Failure = Failure.NetworkFailure("oops")
        val unknown: Failure = Failure.UnknownFailure("oops")

        // Meski message-nya sama, tipe-nya beda
        assertNotEquals(network, unknown)
    }
}
```

COMMENT:
- Verify `open val message` di parent works for both subtypes
- Verify subtype identity (NetworkFailure ≠ UnknownFailure)
- Verify smart cast di when block

---

### Koin DI Verification Test

Test paling penting di core. Pastikan **DI graph utuh** —
semua binding bisa diresolve tanpa missing dependencies.

`commonTest/.../core/modules/AppModulesTest.kt`

```kotlin
class AppModulesTest {

    /**
     * Helper untuk bikin Koin instance yang isolated.
     * Tiap test punya Koin sendiri, gak konflik.
     */
    private fun createKoin() = koinApplication {
        modules(appModules)
    }.koin

    @Test
    fun `HttpClient can be resolved from appModules`() {

        val koin = createKoin()

        // Kalau DI graph rusak, baris ini akan throw exception
        val client = koin.get<HttpClient>()
        assertIs<HttpClient>(client)
    }

    @Test
    fun `BlogRepository is bound to BlogRepositoryImpl`() {

        val koin = createKoin()
        val repository = koin.get<BlogRepository>()

        // Verifikasi interface bound ke implementasi yang benar
        assertIs<BlogRepositoryImpl>(repository)
    }

    @Test
    fun `GetBlogsUseCase resolves entire dependency chain`() {

        val koin = createKoin()

        // UseCase butuh BlogRepository
        // → BlogRepository butuh BlogsApi
        // → BlogsApi butuh HttpClient
        // Kalau ada yang missing, get() bakal throw
        val useCase = koin.get<GetBlogsUseCase>()
        assertIs<GetBlogsUseCase>(useCase)
    }

    @Test
    fun `HttpClient is singleton - same instance returned`() {

        val koin = createKoin()

        val first = koin.get<HttpClient>()
        val second = koin.get<HttpClient>()

        // single { ... } harus return instance yang sama
        assertSame(first, second, "HttpClient harus singleton")
    }

    @Test
    fun `GetBlogsUseCase is factory - different instances`() {

        val koin = createKoin()

        val first = koin.get<GetBlogsUseCase>()
        val second = koin.get<GetBlogsUseCase>()

        // factory { ... } harus return instance baru
        assertNotSame(first, second, "UseCase harus factory")
    }
}
```

COMMENT:
- `koinApplication { }` (bukan `startKoin`) → isolated Koin
  instance per test, gak modifikasi global state
- Resolving chained dependencies (`GetBlogsUseCase`) =
  verifikasi seluruh DI graph
- Test `assertSame` / `assertNotSame` untuk verify scope
  (single vs factory)

### Kenapa Test Koin DI Penting?

| Tanpa test ini | Dengan test ini |
|---|---|
| Lupa register dependency → app crash di runtime | Compile-time-ish detection |
| Wrong binding (Impl ≠ Interface) → wrong behavior | Verified explicitly |
| Refactor module → mungkin missing binding | Test fail, langsung tahu |

---

## 6. API Test dengan MockEngine (Optional)

Kalau mau test BlogsApi langsung (verify URL + parsing JSON),
pakai Ktor MockEngine:

```kotlin
class BlogsApiTest {

    private fun createMockClient(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient {

        val mockEngine = MockEngine { _ ->
            respond(
                content = response,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )
        }

        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun `getBlogs parses JSON correctly`() = runTest {

        val json = """[
            {"userId":1,"id":1,"title":"test","body":"body"}
        ]"""

        val client = createMockClient(json)
        val api = BlogsApiImpl(client)

        val result = api.getBlogs()

        assertEquals(1, result.size)
        assertEquals("test", result[0].title)
    }
}
```

COMMENT:
- MockEngine ≈ Dio mock adapter
- Bisa test URL request, headers, body sekalian
- Jarang dibutuhkan kalau pakai Fake di test Repository

---

## Recommended Test Architecture

```txt
shared/src/commonTest/kotlin/.../
├── core/
│   ├── util/
│   │   ├── ResultTest.kt
│   │   └── FailureTest.kt
│   └── modules/
│       └── AppModulesTest.kt          ← Koin DI verification
│
└── features/
    └── blogs/
        ├── fakes/
        │   ├── FakeBlogsApi.kt
        │   └── FakeBlogRepository.kt
        ├── data/
        │   └── repository/
        │       └── BlogRepositoryImplTest.kt
        ├── domain/
        │   └── usecases/
        │       ├── GetBlogsUseCaseTest.kt
        │       └── GetBlogByIdUseCaseTest.kt
        └── presentations/
            ├── list/
            │   └── BlogViewModelTest.kt
            └── detail/
                └── BlogDetailViewModelTest.kt
```

COMMENT:
- Test di `commonTest` jalan di **semua platform**
  (Android + iOS + Desktop)
- Bisa juga `androidUnitTest`, `iosTest`, `jvmTest`
  untuk platform-specific test (misal yang pakai MockK)

---

## Tips Buat Yang Baru Belajar

| Tips | Penjelasan |
|---|---|
| Mulai dari UseCase test | Paling simple, mudah dipahami |
| Setiap test 1 skenario | Jangan campur banyak assertion |
| Test name = `kondisi_aksi_hasil` | Pakai backtick + spasi |
| Setup di `@BeforeTest` | Fresh state tiap test |
| `runTest { }` selalu | Untuk test suspend function |
| Pakai Given-When-Then comment | Bikin test self-documenting |
| Test happy path dulu | Sebelum edge case |

---

## Final Mental Mapping

| Flutter | KMP |
|---|---|
| `flutter_test` | `kotlin.test` |
| `mockito` | Manual Fakes |
| `bloc_test` | Turbine |
| `expectLater` | `awaitItem` |
| `pump()` | `runCurrent()` |
| `Cubit test` | `ViewModel + Turbine` |
| `when()` | Set property di Fake |
| `verify()` | Counter property di Fake |

---

## Navigasi Docs

- [⬅ 06 — Dependency Injection](06-dependency-injection.md)
- [➡ 08 — Navigation](08-navigation.md)
