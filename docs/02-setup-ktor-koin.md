# 02 — Setup Ktor & Koin (Step by Step)

Section ini setara dengan setup `dio` + `get_it` +
`injectable` di project Flutter.

---

## Step 1. Tambah Dependency di Version Catalog

File: `gradle/libs.versions.toml`

```toml
[versions]
ktor = "2.3.12"
koin = "3.5.6"
koinCompose = "1.1.5"
kotlinxSerialization = "1.6.3"
kotlinxCoroutines = "1.8.1"
multiplatformSettings = "1.1.1"

[libraries]
ktor-client-core = {
    module = "io.ktor:ktor-client-core",
    version.ref = "ktor"
}
ktor-client-content-negotiation = {
    module = "io.ktor:ktor-client-content-negotiation",
    version.ref = "ktor"
}
ktor-serialization-json = {
    module = "io.ktor:ktor-serialization-kotlinx-json",
    version.ref = "ktor"
}
ktor-client-logging = {
    module = "io.ktor:ktor-client-logging",
    version.ref = "ktor"
}
ktor-client-auth = {
    module = "io.ktor:ktor-client-auth",
    version.ref = "ktor"
}
# ↑ optional, hanya kalau butuh Bearer + auto refresh
ktor-client-android = {
    module = "io.ktor:ktor-client-android",
    version.ref = "ktor"
}
ktor-client-darwin = {
    module = "io.ktor:ktor-client-darwin",
    version.ref = "ktor"
}

koin-core = {
    module = "io.insert-koin:koin-core",
    version.ref = "koin"
}
koin-android = {
    module = "io.insert-koin:koin-android",
    version.ref = "koin"
}
koin-compose = {
    module = "io.insert-koin:koin-compose",
    version.ref = "koinCompose"
}

kotlinx-serialization-json = {
    module = "org.jetbrains.kotlinx:kotlinx-serialization-json",
    version.ref = "kotlinxSerialization"
}

multiplatform-settings = {
    module = "com.russhwolf:multiplatform-settings-no-arg",
    version.ref = "multiplatformSettings"
}
```

COMMENT:
- ktor-client-android = engine khusus Android
- ktor-client-darwin = engine khusus iOS / macOS
- Mirip seperti dio_http2_adapter di Flutter

---

## Step 2. Tambah Plugin di Module shared

File: `shared/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        commonMain.dependencies {

            implementation(
                libs.ktor.client.core
            )
            implementation(
                libs.ktor.client.content.negotiation
            )
            implementation(
                libs.ktor.serialization.json
            )
            implementation(
                libs.ktor.client.logging
            )

            // Optional, hanya kalau pakai auth
            // implementation(libs.ktor.client.auth)

            implementation(libs.koin.core)

            implementation(
                libs.kotlinx.serialization.json
            )

            // Optional, hanya kalau pakai
            // token storage / shared prefs
            // implementation(
            //     libs.multiplatform.settings
            // )
        }

        androidMain.dependencies {

            implementation(
                libs.ktor.client.android
            )
            implementation(libs.koin.android)
        }

        iosMain.dependencies {

            implementation(
                libs.ktor.client.darwin
            )
        }
    }
}
```

COMMENT:
- commonMain = kode yang shared (mirip lib/ di Flutter)
- androidMain = engine Ktor khusus Android
- iosMain = engine Ktor khusus iOS
- expect / actual ≈ platform channel di Flutter

---

## Step 3. Buat HttpClient (Factory Pattern)

Versi awal **tanpa auth** — cocok kalau API kamu
belum butuh login. Nanti kalau sudah ada login,
lihat [Data Layer - Auth Token Setup](03-data-layer.md#auth-token-setup-dio-interceptor-equivalent).

File: `shared/core/network/HttpClientFactory.kt`

```kotlin
object HttpClientFactory {

    fun create(): HttpClient {

        return HttpClient {

            expectSuccess = true

            install(ContentNegotiation) {

                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                    }
                )
            }

            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            defaultRequest {

                url("https://api.example.com/v1/")

                contentType(
                    ContentType.Application.Json
                )

                header(
                    HttpHeaders.Accept,
                    "application/json"
                )
            }
        }
    }
}
```

COMMENT:
- expectSuccess = true → auto throw exception
  saat status non-2xx (mirip Dio validateStatus)
- ContentNegotiation + json → auto serialize /
  deserialize JSON (mirip Dio JsonInterceptor)
- Logging plugin ≈ PrettyDioLogger
- HttpTimeout ≈ BaseOptions.connectTimeout
- defaultRequest ≈ BaseOptions (baseUrl + headers)

---

## Mapping Dio BaseOptions → Ktor

| Dio (Flutter) | Ktor (KMP) |
|---|---|
| `BaseOptions.baseUrl` | `defaultRequest { url(...) }` |
| `BaseOptions.headers` | `defaultRequest { header(...) }` |
| `BaseOptions.contentType` | `defaultRequest { contentType(...) }` |
| `BaseOptions.connectTimeout` | `HttpTimeout.connectTimeoutMillis` |
| `BaseOptions.receiveTimeout` | `HttpTimeout.socketTimeoutMillis` |
| `BaseOptions.validateStatus` | `expectSuccess = true` |
| `PrettyDioLogger` | `install(Logging)` |

⚠️ Catatan trailing slash:

```kotlin
defaultRequest { url("https://api.example.com/v1/") }

client.get("users")       // ✅ /v1/users
client.get("/users")      // ❌ /users (slash awal = absolute!)
```

Base URL **selalu** diakhiri `/`, path **jangan**
diawali `/`.

---

## Step 4. Initializer Koin (Lintas Platform)

File: `shared/core/di/KoinInitializer.kt`

```kotlin
fun initKoin(
    appDeclaration: KoinAppDeclaration = {}
) {

    startKoin {

        appDeclaration()

        modules(appModules)
    }
}
```

COMMENT:
- appDeclaration dipakai Android untuk
  inject androidContext()
- Untuk detail Koin module per feature,
  lihat [06 — Dependency Injection](06-dependency-injection.md)

---

## Step 5. Init di Android

File: `androidApp/MainApplication.kt`

```kotlin
class MainApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        initKoin {

            androidLogger()
            androidContext(this@MainApplication)
        }
    }
}
```

Lalu daftarkan di `AndroidManifest.xml`:

```xml
<application
    android:name=".MainApplication"
    ...>
</application>
```

COMMENT:
- Mirip dengan main() di Flutter:
  await configureDependencies();
  runApp(MyApp());

---

## Step 6. Init di iOS

File: `iosApp/iOSApp.swift`

```swift
@main
struct iOSApp: App {

    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

COMMENT:
- doInitKoin() = auto-generated dari fungsi
  initKoin() di commonMain
- Prefix "do" muncul karena Swift interop
- ⚠️ JANGAN panggil initKoin() di MainViewController
  karena akan terpanggil setiap ViewController dibuat

---

## Step 7. Init di Desktop

File: `desktopApp/main.kt`

```kotlin
fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "App",
    ) {
        App()
    }
}
```

---

## Step 8. Pakai di ViewModel / Compose

```kotlin
@Composable
fun LoginScreen() {

    val viewModel: LoginViewModel = koinViewModel()

    val state by viewModel
        .state
        .collectAsStateWithLifecycle()

    Text(state.email)
}
```

COMMENT:
- koinViewModel() ≈ context.read<Bloc>() di
  flutter_bloc
- Atau context.get<T>() di get_it

---

## Step 9. Platform-Specific Engine (Penting!)

Ini bedanya Ktor dengan Dio. Di Flutter, Dio
satu package jalan di semua platform karena
Dart compile ke native binary masing-masing.

Di KMP, **HTTP request perlu engine native
per platform** karena tiap OS punya HTTP stack
sendiri (OkHttp di Android, NSURLSession di iOS).
Jadi Ktor dipecah jadi:

- `ktor-client-core` → API & DSL (shared)
- `ktor-client-<engine>` → engine native per platform

---

### Engine yang Tersedia

| Platform | Engine | Pakai apa di balik layar |
|---|---|---|
| Android | `ktor-client-android` | `HttpURLConnection` (default JVM) |
| Android | `ktor-client-okhttp` | OkHttp (recommended, support HTTP/2) |
| iOS / macOS | `ktor-client-darwin` | NSURLSession |
| Desktop / JVM | `ktor-client-cio` | Pure Kotlin (Coroutine-based IO) |
| Desktop / JVM | `ktor-client-java` | Java 11 HttpClient |
| Desktop / JVM | `ktor-client-okhttp` | OkHttp |
| JS / Browser | `ktor-client-js` | Browser Fetch API |
| WasmJS | `ktor-client-js` | Browser Fetch API |

COMMENT:
- Untuk Android, **OkHttp lebih recommended**
  daripada Android engine (HTTP/2, interceptor
  OkHttp, connection pooling lebih bagus)
- Darwin = nama Apple OS family (iOS, macOS,
  tvOS, watchOS) - semua pakai engine yang sama

---

### Setup Lengkap di build.gradle.kts

```kotlin
kotlin {

    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {

        commonMain.dependencies {

            // Engine-agnostic, shared semua platform
            implementation(libs.ktor.client.core)

            implementation(
                libs.ktor.client.content.negotiation
            )

            implementation(
                libs.ktor.serialization.json
            )

            implementation(
                libs.ktor.client.logging
            )
        }

        androidMain.dependencies {

            // Pakai OkHttp (recommended)
            implementation(
                libs.ktor.client.okhttp
            )

            // Atau, kalau mau yg lebih ringan:
            // implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {

            implementation(
                libs.ktor.client.darwin
            )
        }

        val desktopMain by getting

        desktopMain.dependencies {

            implementation(
                libs.ktor.client.cio
            )
        }
    }
}
```

COMMENT:
- commonMain TIDAK boleh tahu engine apa yg
  dipakai - itu detail platform
- Engine hanya boleh diimport di sourceSet
  spesifik platform

---

### Kenapa Tidak Bisa Satu Engine Saja?

Karena tiap platform punya restriction berbeda:

| Platform | Kenapa butuh engine khusus |
|---|---|
| iOS | App Store WAJIB pakai NSURLSession untuk ATS (App Transport Security), background fetch, sharing cookie dengan WKWebView |
| Android | Butuh integrasi dengan Context, ConnectivityManager |
| Browser | Tidak bisa pakai socket langsung, harus pakai Fetch API |

Jadi engine = adapter native, sementara
`ktor-client-core` adalah API umumnya.

---

### Engine Config per Platform (expect / actual)

Kalau butuh config beda per platform (misal
proxy di Android tapi tidak di iOS):

File: `commonMain/HttpClientEngineConfig.kt`

```kotlin
internal expect fun createEngine(): HttpClientEngine

fun createHttpClient(): HttpClient =
    HttpClient(createEngine()) {

        applyCommonConfig()
    }

internal fun HttpClientConfig<*>.applyCommonConfig() {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.BODY
    }
}
```

File: `androidMain/HttpClientEngineConfig.android.kt`

```kotlin
internal actual fun createEngine(): HttpClientEngine =
    OkHttp.create()
```

File: `iosMain/HttpClientEngineConfig.ios.kt`

```kotlin
internal actual fun createEngine(): HttpClientEngine =
    Darwin.create()
```

File: `jvmMain/HttpClientEngineConfig.jvm.kt`

```kotlin
internal actual fun createEngine(): HttpClientEngine =
    CIO.create()
```

COMMENT:
- expect / actual ≈ platform channel di Flutter
- Tapi compile-time, bukan runtime
- Lebih type-safe & performant

---

### Cheat Sheet: Pilih Engine Apa?

| Kebutuhan | Engine |
|---|---|
| Android, simple project | `ktor-client-android` |
| Android, butuh HTTP/2 + interceptor OkHttp | `ktor-client-okhttp` ✅ Recommended |
| iOS / macOS (wajib) | `ktor-client-darwin` |
| Desktop, pure Kotlin | `ktor-client-cio` |
| Desktop, kompatibel sama OkHttp client lain | `ktor-client-okhttp` |
| Browser / WasmJS | `ktor-client-js` |

---

## Mapping Flutter Setup → KMP Setup

| Flutter | KMP |
|---|---|
| pubspec.yaml | gradle/libs.versions.toml |
| dio: ^5.0 | ktor-client-core |
| dio.options.baseUrl | defaultRequest.url |
| dio.interceptors.add | install(Auth) / install(Logging) |
| get_it | Koin |
| injectable | Koin module manual |
| @injectable | single { } / singleOf(::) |
| @lazySingleton | single { } |
| @factoryMethod | factory { } / factoryOf(::) |
| configureDependencies() | startKoin { modules(...) } |
| getIt<T>() | get() / koinInject() / koinViewModel() |
| main.dart | MainApplication.kt + iOSApp.swift |

---

## Navigasi Docs

- [⬅ 01 — Overview](01-overview.md)
- [➡ 03 — Data Layer](03-data-layer.md)
- [04 — Domain Layer](04-domain-layer.md)
- [05 — Presentation Layer (MVI)](05-presentation-layer-mvi.md)
- [06 — Dependency Injection](06-dependency-injection.md)
- [07 — Testing](07-testing.md)
