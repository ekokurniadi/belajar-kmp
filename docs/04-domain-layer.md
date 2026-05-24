# 04 — Domain Layer

Layer paling penting di Clean Architecture. **Pure
business logic** — tidak boleh tahu API, JSON, atau
framework apapun.

Layer ini berisi:

- **Domain Model** (entity, pure data class)
- **Result Wrapper** (sealed class success/error)
- **Failure** (typed error sealed class)
- **Repository Interface** (abstract dari data source)
- **UseCase** (1 aksi = 1 usecase)

---

## 1. Domain Model

Entity di domain layer — bersih, tanpa JSON,
tanpa `@Serializable`.

`features/blogs/domain/model/BlogModel.kt`

```kotlin
data class BlogModel(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)
```

COMMENT:
- **TIDAK ada** `@Serializable`
- **TIDAK boleh** depend ke library JSON apapun
- Bisa pakai tipe yg lebih domain-friendly (LocalDateTime vs String)
- Field bisa di-rename (fullName → name) di mapper

---

## 2. Result Wrapper (dartz Either Equivalent)

### Flutter

```dart
Either<Failure, User>
```

### KMP

`core/util/Result.kt`

```kotlin
sealed class Result<out T> {

    data class Success<T>(
        val data: T
    ) : Result<T>()

    data class Error(
        val failure: Failure
    ) : Result<Nothing>()
}
```

COMMENT:
- `sealed class` ≈ enum-with-data di Dart
- `Result.Success<T>` ≈ `Right<T>` di dartz
- `Result.Error` ≈ `Left<Failure>` di dartz
- Wajib pakai `when` exhaustive untuk handle

---

## 3. Failure Pattern

Di Flutter kita biasa bikin `abstract class Failure`
+ subclass. Di Kotlin lebih clean pakai `sealed class`.

### Mapping Flutter → Kotlin

| Flutter (dartz + abstract Failure) | Kotlin (sealed class) |
|---|---|
| `abstract class Failure` | `sealed class Failure` |
| `class XFailure extends Failure` | `data class X : Failure()` |
| `Either<Failure, T>` | `Result<T>` di mana Error punya `Failure` |
| `result.fold(left, right)` | `when (result)` exhaustive |
| `failure is NetworkFailure` | `failure is Failure.Network` (smart cast otomatis) |

### Implementation

`core/util/Failure.kt`

```kotlin
sealed class Failure(
    open val message: String
) {

    data class NetworkFailure(
        override val message: String =
            "Tidak ada koneksi internet"
    ) : Failure(message)

    data class UnknownFailure(
        override val message: String =
            "Terjadi kesalahan tak terduga"
    ) : Failure(message)
}
```

COMMENT:
- `sealed class` → compiler tahu semua subclass
- `data class` → auto equals/hashCode/copy
- `open val` → izinkan subclass override (default Kotlin final)
- `override val` → child override property parent

### Tabel Modifier Kotlin

| Modifier | Arti | Default? |
|---|---|---|
| `final` | Tidak bisa di-override | ✅ Default |
| `open` | Bisa di-override | Manual |
| `abstract` | **Harus** di-override | Manual |
| `override` | Override member parent | Manual |
| `sealed` | Closed inheritance | Manual |
| `data` | Auto equals/hashCode/copy/toString | Manual |

---

### Versi yang Lebih Lengkap

Kalau mau kaya tipe error, bisa diperluas:

```kotlin
sealed class Failure(
    open val message: String
) {

    data class NetworkFailure(
        override val message: String =
            "Tidak ada koneksi internet"
    ) : Failure(message)

    data class TimeoutFailure(
        override val message: String =
            "Koneksi timeout"
    ) : Failure(message)

    data class ServerFailure(
        override val message: String,
        val httpStatus: Int
    ) : Failure(message)

    data class UnauthorizedFailure(
        override val message: String =
            "Sesi habis, silakan login ulang"
    ) : Failure(message)

    data class NotFoundFailure(
        override val message: String =
            "Data tidak ditemukan"
    ) : Failure(message)

    data class ValidationFailure(
        override val message: String,
        val fieldErrors:
            Map<String, List<String>> =
            emptyMap()
    ) : Failure(message)

    data class UnknownFailure(
        override val message: String =
            "Terjadi kesalahan tak terduga"
    ) : Failure(message)
}
```

---

### Helper: Map Exception ke Failure

`core/network/ExceptionMapper.kt`

```kotlin
fun Throwable.toFailure(): Failure =
    when (this) {

        is SocketTimeoutException ->
            Failure.TimeoutFailure()

        is IOException ->
            Failure.NetworkFailure()

        is ClientRequestException -> {
            when (response.status.value) {

                401 -> Failure.UnauthorizedFailure()
                404 -> Failure.NotFoundFailure()

                else -> Failure.ServerFailure(
                    message = message ?: "Request gagal",
                    httpStatus = response.status.value
                )
            }
        }

        is ServerResponseException ->
            Failure.ServerFailure(
                message = "Server bermasalah",
                httpStatus = response.status.value
            )

        else -> Failure.UnknownFailure(
            message = message ?: "Unknown error"
        )
    }
```

COMMENT:
- Extension function `Throwable.toFailure()` ≈ helper static di Flutter
- `when` exhaustive ≈ switch case Dart

---

## 4. Repository Interface

### Flutter

```dart
abstract class AuthRepository {
  Future<Either<Failure, User>> login();
}
```

### KMP

`features/blogs/domain/repository/BlogRepository.kt`

```kotlin
interface BlogRepository {

    suspend fun getBlogs(): Result<List<BlogModel>>
}
```

COMMENT:
- `Future<Either<Failure, T>>` di Flutter
  = `suspend fun ...: Result<T>` di Kotlin
- Interface **di domain layer**, supaya domain
  gak depend ke data
- Return type `Result<...>` (Domain Model, bukan DTO)

---

## 5. UseCase

1 aksi = 1 usecase. Membuat business logic
reusable dan testable.

### Flutter

```dart
class LoginUseCase {
  final AuthRepository repository;

  LoginUseCase(this.repository);

  Future<Either<Failure, User>> call() {
    return repository.login();
  }
}
```

### KMP — Simple Pattern (Recommended)

`features/blogs/domain/usecases/GetBlogsUseCase.kt`

```kotlin
class GetBlogsUseCase(
    private val repository: BlogRepository,
) {

    suspend operator fun invoke(): Result<List<BlogModel>> =
        repository.getBlogs()
}
```

COMMENT:
- `operator fun invoke()` bikin syntax mirip
  function call (`useCase()`)
- Body singkat → pakai `=` expression body
  (idiomatic Kotlin)
- Return type **sama persis** dengan Repository
  (`Result<...>`)

### KMP — Pakai Base Interface (Optional)

Kalau mau punya base interface untuk konsistensi:

`core/usecase/UseCase.kt`

```kotlin
interface UseCase<in P, out R> {

    suspend operator fun invoke(param: P): R
}

interface NoParamUseCase<out R> {

    suspend operator fun invoke(): R
}
```

COMMENT:
- `in P` = contravariant (parameter)
- `out R` = covariant (return)
- `NoParamUseCase` untuk usecase tanpa parameter
- ⚠️ JANGAN pakai `Nothing` sebagai parameter
  karena `Nothing` tidak punya instance,
  artinya usecase tidak pernah bisa dipanggil

Lalu pakai:

```kotlin
class GetBlogsUseCase(
    private val repository: BlogRepository,
) : NoParamUseCase<Result<List<BlogModel>>> {

    override suspend operator fun invoke():
        Result<List<BlogModel>> =
        repository.getBlogs()
}
```

### UseCase dengan Parameter

```kotlin
class GetBlogByIdUseCase(
    private val repository: BlogRepository
) {

    suspend operator fun invoke(
        id: Int
    ): Result<BlogModel> =
        repository.getBlogById(id)
}

// Pakai:
val result = getBlogByIdUseCase(id = 5)
```

### UseCase dengan Multi-Parameter

```kotlin
class SearchBlogsUseCase(
    private val repository: BlogRepository
) {

    data class Params(
        val query: String,
        val page: Int = 1,
        val perPage: Int = 20
    )

    suspend operator fun invoke(
        params: Params
    ): Result<List<BlogModel>> =
        repository.searchBlogs(
            params.query,
            params.page,
            params.perPage
        )
}

// Pakai:
val result = searchBlogsUseCase(
    SearchBlogsUseCase.Params(
        query = "kotlin",
        page = 1
    )
)
```

---

## 6. Mapping Konsep Kunci

### Kotlin Collection vs Dart

| Dart (Flutter) | Kotlin (KMP) |
|---|---|
| `list.map((e) => e.toEntity()).toList()` | `list.map { it.toDomain() }` |
| `list.where((e) => e.isActive).toList()` | `list.filter { it.isActive }` |
| `list.firstWhere((e) => ..., orElse: ...)` | `list.firstOrNull { ... }` |
| `list.isEmpty` | `list.isEmpty()` |
| `[]` (empty list) | `emptyList()` |

### Domain Layer Best Practices

| Hal | Aturan |
|---|---|
| Import library JSON | ❌ Tidak boleh |
| Import Ktor | ❌ Tidak boleh |
| Import Android/iOS native | ❌ Tidak boleh |
| Pakai data class biasa | ✅ Ya |
| Pakai LocalDateTime | ✅ Ya (kotlinx-datetime) |
| Repository interface | ✅ Ya |
| Implementasi Repository | ❌ Tidak, itu di data layer |

---

## Navigasi Docs

- [⬅ 03 — Data Layer](03-data-layer.md)
- [➡ 05 — Presentation Layer (MVI)](05-presentation-layer-mvi.md)
- [06 — Dependency Injection](06-dependency-injection.md)
- [07 — Testing](07-testing.md)
