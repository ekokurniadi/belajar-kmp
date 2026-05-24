# 06 — Dependency Injection (Koin)

Koin di KMP setara dengan **`get_it` + `injectable`**
di Flutter. Kelebihan: tidak perlu codegen, manual
tapi tetap clean.

---

## 1. Konsep Dasar

### Flutter

```dart
getIt.registerLazySingleton<AuthRepository>(
  () => AuthRepositoryImpl(),
);
```

### KMP

```kotlin
val appModule = module {

    single<AuthRepository> {
        AuthRepositoryImpl(get())
    }

    factory {
        LoginUseCase(get())
    }

    viewModel {
        LoginViewModel(get())
    }
}
```

COMMENT:
- `get()` pada Koin ≈ `getIt<T>()`
- `single` = singleton (≈ `registerLazySingleton`)
- `factory` = new instance per call (≈ `registerFactory`)
- `viewModel` = scoped ViewModel

---

## 2. Modular DI per Feature (Best Practice)

Saat project nambah, taruh semua binding di satu
file bakal berantakan. Best practice: **1 modul
per feature**, lalu di-include ke `appModules`.

### Kenapa Per Feature?

| Tanpa per-feature | Dengan per-feature |
|---|---|
| `networkModule` membengkak | Tiap feature mandiri |
| Susah cari binding | Cari di folder feature-nya |
| Konflik nama saat banyak repo | Scoped per feature |
| Susah hapus feature | Tinggal hapus 1 modul |

### Struktur Folder

```txt
shared/src/commonMain/kotlin/.../
├── core/
│   └── modules/
│       ├── AppModules.kt      ← agregator
│       └── NetworkModule.kt   ← infra (HttpClient)
│
└── features/
    ├── blogs/
    │   ├── data/
    │   ├── domain/
    │   ├── presentations/
    │   └── di/
    │       └── BlogModule.kt
    │
    ├── auth/
    │   ├── data/
    │   ├── domain/
    │   ├── presentations/
    │   └── di/
    │       └── AuthModule.kt
    │
    └── users/
        ├── ...
        └── di/
            └── UsersModule.kt
```

---

## 3. Module per Feature

`features/blogs/di/BlogModule.kt`

```kotlin
val blogModule = module {

    // Data
    single<BlogsApi> {
        BlogsApiImpl(get())
    }

    single<BlogRepository> {
        BlogRepositoryImpl(get())
    }

    // Domain
    factory {
        GetBlogsUseCase(get())
    }

    // Presentation
    factory {
        BlogViewModel(get())
    }
}
```

`features/auth/di/AuthModule.kt`

```kotlin
val authModule = module {

    single<AuthApi> {
        AuthApiImpl(get())
    }

    single<AuthRepository> {
        AuthRepositoryImpl(get())
    }

    factory { LoginUseCase(get()) }
    factory { LogoutUseCase(get()) }

    factory { LoginViewModel(get()) }
}
```

---

## 4. Versi Lebih Clean: Constructor DSL

Koin punya **Constructor DSL** yang bikin
binding lebih ringkas - mirip auto-injection
di injectable Flutter.

```kotlin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf

val blogModule = module {

    // Data
    singleOf(::BlogsApiImpl) {
        bind<BlogsApi>()
    }

    singleOf(::BlogRepositoryImpl) {
        bind<BlogRepository>()
    }

    // Domain
    factoryOf(::GetBlogsUseCase)

    // Presentation
    viewModelOf(::BlogViewModel)
}
```

COMMENT:
- `::ClassName` = reference ke constructor
  (mirip tear-off di Dart)
- Koin auto-resolve semua parameter dari registered binding
- `bind<Interface>()` = expose impl sebagai interface
- Lebih sedikit boilerplate

---

## 5. Tabel Constructor DSL

| Tanpa DSL | Pakai Constructor DSL |
|---|---|
| `single { Impl(get()) }` | `singleOf(::Impl)` |
| `factory { UseCase(get()) }` | `factoryOf(::UseCase)` |
| `single<I> { Impl(get()) }` | `singleOf(::Impl) { bind<I>() }` |
| `viewModel { Vm(get()) }` | `viewModelOf(::Vm)` |
| Multi parameter | `singleOf(::Class)` ✅ auto-resolve semua |

---

## 6. Kapan TETAP Pakai Lambda?

`singleOf` hanya bisa kalau dependency-nya
resolvable dari Koin. Kalau perlu custom
construction, pakai lambda biasa.

```kotlin
val networkModule = module {

    // Factory method, bukan constructor
    single {
        HttpClientFactory.create()
    }

    // Named qualifier
    single(named("base_url")) {
        "https://api.example.com/"
    }

    // Parameter dinamis
    factory { (token: String) ->
        TokenService(token, get())
    }
}
```

---

## 7. Aggregator di Core

`core/modules/AppModules.kt`

```kotlin
val appModules = listOf(

    // Core / Infrastructure
    networkModule,

    // Features
    authModule,
    blogModule,
    usersModule
)
```

COMMENT:
- Saat tambah feature baru, cuma update
  1 file ini
- Urutan tidak masalah (Koin lazy-resolve)

---

## 8. Grouping yang Lebih Rapi (Optional)

```kotlin
private val coreModules = listOf(
    networkModule
)

private val featureModules = listOf(
    authModule,
    blogModule,
    usersModule
)

val appModules =
    coreModules + featureModules
```

---

## 9. Init Tetap Sama

Karena `initKoinAndroid/iOS/Desktop` udah
pakai `modules(appModules)`, **tidak ada
yang perlu diubah** di sisi platform.

```kotlin
fun initKoinAndroid(context: Context) {
    startKoin {
        androidLogger()
        androidContext(context)
        modules(appModules)
    }
}
```

---

## 10. Sub-Module per Feature (Granular)

Kalau feature sangat besar (>20 binding),
bisa pecah lagi per layer:

```txt
features/blogs/
└── di/
    ├── BlogsDataModule.kt       ← Api + Repository
    ├── BlogsDomainModule.kt     ← UseCase
    ├── BlogsPresentationModule.kt ← ViewModel
    └── BlogsModule.kt           ← list gabungan
```

`BlogsModule.kt`:

```kotlin
val blogsModule = listOf(
    blogsDataModule,
    blogsDomainModule,
    blogsPresentationModule
)
```

`AppModules.kt`:

```kotlin
val appModules = listOf(
    networkModule
) + blogsModule + authModule
```

COMMENT:
- Granular pattern berguna kalau:
  - Feature >20 binding
  - Mau lazy-load per layer
  - Mau test inject sub-module spesifik
- Sebagian besar project gak perlu segini

---

## 11. Mengakses Dependency

### Di Compose

```kotlin
@Composable
fun BlogScreen(
    viewModel: BlogViewModel = koinViewModel(),
    // atau
    repository: BlogRepository = koinInject(),
) {
    // ...
}
```

### Di Class Biasa (Injected)

```kotlin
class SomeService(
    private val client: HttpClient,
    private val repository: BlogRepository,
) {
    // Dependency masuk via constructor
}

// Daftar:
single { SomeService(get(), get()) }
// atau:
singleOf(::SomeService)
```

### Di Class Non-Injected (Akses Manual)

```kotlin
class Helper : KoinComponent {

    private val client by inject<HttpClient>()
}
```

COMMENT:
- Recommended: **constructor injection** (auto via `singleOf`)
- Manual `KoinComponent` cuma kalau terpaksa
  (misal class yang gak kamu kontrol)

---

## 12. Mapping injectable Flutter → Koin

| Flutter (injectable) | Koin |
|---|---|
| `@module class XInjectableModule` | `val xModule = module { }` |
| `@Injectable()` | `factoryOf(::Class)` |
| `@LazySingleton()` | `singleOf(::Class)` |
| `@Injectable(as: Interface)` | `singleOf(::Impl) { bind<Interface>() }` |
| `injection.config.dart` (codegen) | `AppModules.kt` (manual, no codegen) |
| `@injectableInit` | `startKoin { modules(appModules) }` |

---

## 13. Convention Penamaan

| Variable | Convention |
|---|---|
| `blogModule` | `<feature>Module`, lowercase |
| `authModule` | sama |
| `appModules` | plural - berisi list |

---

## 14. Cheat Sheet: single vs factory vs viewModel

| Scope | Lifecycle | Pakai untuk |
|---|---|---|
| `single` | Singleton (1 instance se-app) | Repository, ApiService, HttpClient, Database |
| `factory` | Baru tiap kali resolve | UseCase, Helper class |
| `viewModel` | Scoped ke composable lifecycle | ViewModel |

---

## Navigasi Docs

- [⬅ 05 — Presentation Layer (MVI)](05-presentation-layer-mvi.md)
- [➡ 07 — Testing](07-testing.md)
