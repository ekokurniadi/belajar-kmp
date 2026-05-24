# 03 — Data Layer

Layer ini bertanggung jawab atas:

- **DTO** (parsing JSON dari API)
- **Mapper** (DTO ↔ Domain Model)
- **API / Datasource** (Ktor HTTP calls)
- **Repository Implementation** (translate Data → Domain)
- **Auth Token Handling** (opsional)

---

## 1. DTO (Data Transfer Object)

Di Flutter biasanya kita pisah `Model` (parsing API)
dan `Entity` (data internal aplikasi). Di KMP polanya
sama, cuma nama beda.

### Mapping Flutter → KMP

| Flutter | KMP |
|---|---|
| `UserModel` (data layer) | `UserDto` (data layer) |
| `User` (entity, domain layer) | `User` (model/entity, domain layer) |
| `UserModel.fromJson()` | `@Serializable` auto-generate |
| `userModel.toEntity()` | `UserDto.toDomain()` |
| `UserModel.fromEntity()` | `User.toDto()` (optional) |

### Contoh DTO

`features/blogs/data/dto/BlogDto.kt`

```kotlin
@Serializable
data class BlogDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)
```

COMMENT:
- `@Serializable` = `fromJson`/`toJson` di Flutter (auto-generate)
- `@SerialName` = `@JsonKey` di freezed (kalau nama field berbeda)
- Default value (`= null`) = nullable safety dari backend

### DTO dengan @SerialName

```kotlin
@Serializable
data class UserDto(

    @SerialName("user_id")
    val id: Int,

    @SerialName("full_name")
    val fullName: String,

    @SerialName("email_address")
    val email: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
```

---

## 2. Mapper (DTO → Domain)

Extension function = idiomatic Kotlin untuk mapping.

`features/blogs/data/mapper/BlogDtoMapper.kt`

```kotlin
fun BlogDto.toDomain(): BlogModel = BlogModel(
    userId = userId,
    id = id,
    title = title,
    body = body
)
```

COMMENT:
- Extension function mirip `userModel.toEntity()` di Flutter
- File mapper boleh berisi banyak extension untuk satu domain

### Mapper untuk List

Kalau capek tulis `.map { it.toDomain() }` terus,
bikin extension untuk List:

```kotlin
fun List<BlogDto>.toDomain(): List<BlogModel> =
    map { it.toDomain() }
```

Pemakaian:

```kotlin
Result.Success(dtos.toDomain())
// Bukan: Result.Success(dtos.map { it.toDomain() })
```

COMMENT:
- Function name **boleh sama** dengan `BlogDto.toDomain()` karena receiver-nya beda (List vs single)
- Kotlin compiler bedakan via receiver type

---

## 3. Ktor API (Dio Equivalent)

### Flutter

```dart
final response = await dio.post('/login');
```

### KMP

`features/blogs/data/datasources/api/BlogsApi.kt`

```kotlin
interface BlogsApi {

    suspend fun getBlogs(): List<BlogDto>
}

class BlogsApiImpl(
    private val httpClient: HttpClient
) : BlogsApi {

    override suspend fun getBlogs(): List<BlogDto> =
        httpClient.get("posts").body()
}
```

COMMENT:
- HttpClient pada Ktor ≈ Dio
- `body()` ≈ `response.data`
- API return **raw DTO**, gak ada `Result`
  (`Result` adalah tanggung jawab Repository)
- Pakai path relatif (`"posts"`, bukan
  `"/posts"`) karena base URL sudah di-set
  di `defaultRequest`

---

## 4. List & Pagination

### Case 1: Response Berupa Array Langsung

```json
[
  { "user_id": 1, "full_name": "Eko" },
  { "user_id": 2, "full_name": "Budi" }
]
```

```kotlin
class UserApi(
    private val client: HttpClient
) {

    suspend fun getUsers(): List<UserDto> =
        client.get("users").body()
}
```

COMMENT:
- `body()` smart enough untuk parse `List<UserDto>`
- kotlinx-serialization auto-handle array

### Case 2: Response Wrapped (Paling Sering)

```json
{
  "data": [
    { "user_id": 1, "full_name": "Eko" }
  ],
  "meta": {
    "total": 50,
    "page": 1,
    "per_page": 20
  }
}
```

**DTO wrapper:**

```kotlin
@Serializable
data class UserListResponseDto(

    @SerialName("data")
    val data: List<UserDto>,

    @SerialName("meta")
    val meta: MetaDto
)

@Serializable
data class MetaDto(
    val total: Int,
    val page: Int,

    @SerialName("per_page")
    val perPage: Int
)
```

**Domain (pakai class wrapper kalau perlu paging info):**

```kotlin
data class PagedUsers(
    val users: List<User>,
    val totalItems: Int,
    val currentPage: Int,
    val perPage: Int
) {

    val hasNextPage: Boolean
        get() = currentPage * perPage < totalItems
}
```

COMMENT:
- Property `hasNextPage` = computed property (mirip getter di Dart)
- Domain bisa tambah logic yg gak ada di DTO

**Mapper:**

```kotlin
fun UserListResponseDto.toDomain(): PagedUsers =
    PagedUsers(
        users = data.map { it.toDomain() },
        totalItems = meta.total,
        currentPage = meta.page,
        perPage = meta.perPage
    )
```

---

## 5. Repository Implementation

Repository = boundary tempat translation Data → Domain.

`features/blogs/data/repository/BlogRepositoryImpl.kt`

```kotlin
class BlogRepositoryImpl(
    private val blogsApi: BlogsApi,
) : BlogRepository {

    override suspend fun getBlogs(): Result<List<BlogModel>> {

        return try {

            val response: List<BlogDto> =
                blogsApi.getBlogs()

            Result.Success(
                response.map { it.toDomain() }
            )

        } catch (e: Exception) {

            Result.Error(
                failure = Failure.NetworkFailure(
                    message = e.message ?: "Unknown Error"
                )
            )
        }
    }
}
```

COMMENT:
- API return `Dto`, repository return `Domain Model`
- Repository = boundary tempat translation terjadi
- UseCase & ViewModel cuma tahu `BlogModel`, gak tahu `BlogDto`
- Untuk detail `Failure` & `Result`, lihat
  [04 — Domain Layer](04-domain-layer.md)

---

## Auth Token Setup (Dio Interceptor Equivalent)

Di Flutter biasanya kita setup token via Dio interceptor
atau BaseOptions.headers. Di Ktor pendekatannya menggunakan
plugin Auth (untuk Bearer + auto refresh) atau defaultRequest
(untuk header statis).

### Flutter (Dio)

```dart
dio.options.headers['Authorization'] =
    'Bearer $token';

dio.interceptors.add(
  InterceptorsWrapper(
    onRequest: (options, handler) {
      options.headers['Authorization'] =
          'Bearer ${storage.token}';
      handler.next(options);
    },
    onError: (e, handler) async {
      if (e.response?.statusCode == 401) {
        await refreshToken();
      }
      handler.next(e);
    },
  ),
);
```

### Mapping Dio → Ktor

| Dio (Flutter) | Ktor (KMP) |
|---|---|
| BaseOptions.headers | defaultRequest |
| InterceptorsWrapper.onRequest | HttpSend.intercept |
| onError 401 refresh | Auth.bearer.refreshTokens |
| dio.options.baseUrl | defaultRequest.url |

---

### 1. Token Storage

Pakai multiplatform-settings supaya bisa di
Android, iOS, Desktop.

```kotlin
class TokenStorage(
    private val settings: Settings
) {

    var accessToken: String?
        get() = settings.getStringOrNull("access_token")
        set(value) {

            if (value == null) {
                settings.remove("access_token")
            } else {
                settings.putString(
                    "access_token",
                    value
                )
            }
        }

    var refreshToken: String?
        get() = settings.getStringOrNull("refresh_token")
        set(value) {

            if (value == null) {
                settings.remove("refresh_token")
            } else {
                settings.putString(
                    "refresh_token",
                    value
                )
            }
        }

    fun clear() {
        accessToken = null
        refreshToken = null
    }
}
```

COMMENT:
- multiplatform-settings ≈ shared_preferences di Flutter
- Bisa juga pakai DataStore Multiplatform

---

### 2. Ktor Client dengan Auth Plugin

```kotlin
val client = HttpClient {

    install(ContentNegotiation) {
        json()
    }

    install(Auth) {

        bearer {

            loadTokens {

                val access = tokenStorage.accessToken
                val refresh = tokenStorage.refreshToken

                if (access != null) {

                    BearerTokens(
                        accessToken = access,
                        refreshToken = refresh ?: ""
                    )

                } else null
            }

            refreshTokens {

                val response = client.post(
                    "https://api.example.com/refresh"
                ) {

                    markAsRefreshTokenRequest()

                    contentType(
                        ContentType.Application.Json
                    )

                    setBody(
                        RefreshRequest(
                            tokenStorage.refreshToken!!
                        )
                    )

                }.body<TokenResponse>()

                tokenStorage.accessToken = response.access
                tokenStorage.refreshToken = response.refresh

                BearerTokens(
                    accessToken = response.access,
                    refreshToken = response.refresh
                )
            }

            sendWithoutRequest { request ->
                request.url.host == "api.example.com"
            }
        }
    }
}
```

COMMENT:
- loadTokens dipanggil sekali, hasilnya di-cache
- refreshTokens auto trigger saat dapat 401
- markAsRefreshTokenRequest cegah infinite loop
- sendWithoutRequest = kirim token langsung
  tanpa nunggu 401 challenge

---

### 3. Login Flow

Setelah login berhasil, **WAJIB** clearToken()
supaya loadTokens di-trigger ulang dengan
token baru.

```kotlin
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> {

        return try {

            val response = api.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

            tokenStorage.accessToken = response.accessToken
            tokenStorage.refreshToken = response.refreshToken

            client
                .plugin(Auth)
                .providers
                .filterIsInstance<BearerAuthProvider>()
                .forEach { it.clearToken() }

            Result.Success(response.user.toDomain())

        } catch (e: Exception) {
            Result.Error(
                failure = Failure.UnknownFailure(
                    message = e.message ?: "Unknown Error"
                )
            )
        }
    }

    override suspend fun logout() {

        tokenStorage.clear()

        client
            .plugin(Auth)
            .providers
            .filterIsInstance<BearerAuthProvider>()
            .forEach { it.clearToken() }
    }
}
```

COMMENT:
- clearToken() WAJIB setelah login/logout
- Kalau tidak, Ktor pakai token lama yang
  di-cache di memory
- Mirip Dio.interceptors.clear() lalu set
  ulang headers

---

### 4. Penggunaan di Request Berikutnya

Setelah login, semua request otomatis
membawa header Authorization.

```kotlin
val profile = client
    .get("me")
    .body<Profile>()
```

COMMENT:
- Header `Authorization: Bearer <token>`
  otomatis ditambahkan
- Tidak perlu manual set header per request

---

### Alternatif Sederhana (Tanpa Auth Plugin)

Kalau API hanya pakai static token (API key)
atau tidak butuh auto refresh, cukup pakai
defaultRequest.

```kotlin
val client = HttpClient {

    install(DefaultRequest) {

        tokenStorage.accessToken?.let {
            header(
                HttpHeaders.Authorization,
                "Bearer $it"
            )
        }

        contentType(ContentType.Application.Json)

        url("https://api.example.com/")
    }
}
```

COMMENT:
- Mirip BaseOptions di Dio
- Header dibaca ulang setiap request

---

### Custom Interceptor (Mirip Dio Interceptor)

Kalau butuh logic lebih kompleks, pakai
HttpSend plugin.

```kotlin
client.plugin(HttpSend).intercept { request ->

    request.headers.append(
        "Authorization",
        "Bearer ${tokenStorage.accessToken}"
    )

    val call = execute(request)

    if (call.response.status.value == 401) {
        // handle refresh manual
    }

    call
}
```

COMMENT:
- HttpSend.intercept ≈ Dio InterceptorsWrapper
- execute(request) ≈ handler.next()

---

### Catatan Penting Auth

| Hal | Penjelasan |
|---|---|
| clearToken() | WAJIB setelah login/logout supaya loadTokens dipanggil ulang |
| markAsRefreshTokenRequest() | Cegah infinite loop saat refresh endpoint kena 401 |
| sendWithoutRequest | Default Ktor baru kirim token setelah 401 challenge. Set true untuk kirim langsung |
| Token storage | Pakai multiplatform-settings atau DataStore agar cross platform |

---

## Custom Error Handling

Kalau API kamu return custom error message
(misal `{"code": "...", "message": "..."}`),
handle di datasource dengan throw exception,
lalu Repository convert ke `Result.Error` dengan
`Failure` yang tepat.

### Pembagian Tanggung Jawab

| Layer | Tugas |
|---|---|
| **API / Datasource** | Parse error body → throw **typed exception** |
| **Repository** | Catch exception → convert ke `Result.Error` dengan `Failure` |
| **UseCase** | Pass-through |
| **ViewModel** | Tampilkan error ke UI |

### Safe API Call Helper

```kotlin
suspend inline fun <reified T> safeApiCall(
    crossinline block: suspend () -> T
): T {

    return try {

        block()

    } catch (e: ClientRequestException) {
        // 4xx
        throw mapToAppException(
            e,
            e.response.status.value
        )

    } catch (e: ServerResponseException) {
        // 5xx
        throw AppException.ApiException(
            code = null,
            httpStatus = e.response.status.value,
            message = "Server sedang bermasalah"
        )

    } catch (e: SocketTimeoutException) {
        throw AppException.NetworkException(
            "Koneksi timeout"
        )

    } catch (e: IOException) {
        throw AppException.NetworkException()

    } catch (e: Exception) {
        throw AppException.UnknownException(
            cause = e
        )
    }
}
```

### Pakai di Datasource

```kotlin
class BlogsApiImpl(
    private val httpClient: HttpClient
) : BlogsApi {

    override suspend fun getBlogs(): List<BlogDto> =
        safeApiCall {
            httpClient.get("posts").body()
        }
}
```

COMMENT:
- Tipis & deklaratif
- Semua error handling dibungkus `safeApiCall`
- Datasource fokus ke **apa**, bukan **bagaimana handle error**

Untuk detail `Failure` & `AppException`, lihat
[04 — Domain Layer](04-domain-layer.md#failure-pattern).

---

## Navigasi Docs

- [⬅ 02 — Setup Ktor & Koin](02-setup-ktor-koin.md)
- [➡ 04 — Domain Layer](04-domain-layer.md)
- [05 — Presentation Layer (MVI)](05-presentation-layer-mvi.md)
- [06 — Dependency Injection](06-dependency-injection.md)
- [07 — Testing](07-testing.md)
