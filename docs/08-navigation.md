# 08 — Navigation

Compose Multiplatform pakai **`androidx-navigation-compose`**
(versi multiplatform dari JetBrains). Sejak versi 2.8+ ada
**type-safe routes** yang lebih clean daripada string-based
routes.

---

## Mapping go_router (Flutter) → Compose Navigation

| Flutter (go_router) | KMP (Compose Navigation) |
|---|---|
| `GoRouter(routes: [...])` | `NavHost(navController, startDestination = ...) {...}` |
| `GoRoute(path: '/blogs')` | `composable<AppRoute.BlogList> { ... }` |
| `GoRoute(path: '/blogs/:id')` | `composable<AppRoute.BlogDetail> { ... }` (dengan data class) |
| `context.push('/blogs/$id')` | `navController.navigate(AppRoute.BlogDetail(id))` |
| `context.pop()` | `navController.popBackStack()` |
| `GoRouterState.params['id']` | `backStackEntry.toRoute<AppRoute.BlogDetail>().blogId` |
| Type-safe param | Otomatis lewat `@Serializable` data class |

---

## 1. Dependency

`gradle/libs.versions.toml`

```toml
[versions]
androidx-navigation = "2.9.2"

[libraries]
androidx-navigation-compose = {
    module = "org.jetbrains.androidx.navigation:navigation-compose",
    version.ref = "androidx-navigation"
}
```

`shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.androidx.navigation.compose)
}
```

COMMENT:
- Pakai `org.jetbrains.androidx.navigation` (versi multiplatform)
- **BUKAN** `androidx.navigation` (versi Android-only)
- Type-safe routes butuh navigation-compose 2.8+

---

## 2. Type-Safe Routes (Recommended)

Di Flutter biasanya kita pakai string path: `/blogs/123`.
Di Compose Navigation modern, kita pakai **`@Serializable`
data class** sebagai route. Type-safe + autocomplete +
refactor-friendly.

`core/navigation/AppNavigation.kt`

```kotlin
import kotlinx.serialization.Serializable

sealed interface AppRoute {

    @Serializable
    data object BlogList : AppRoute

    @Serializable
    data class BlogDetail(val blogId: Int) : AppRoute
}
```

COMMENT:
- `sealed interface` → grouping semua route dalam 1 namespace
- `@Serializable` (kotlinx-serialization) → required oleh
  navigation library untuk encode/decode parameter
- `data object` untuk route tanpa param
- `data class` untuk route dengan param

---

## 3. NavHost (Navigation Graph)

```kotlin
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.BlogList,
    ) {

        composable<AppRoute.BlogList> {
            BlogScreen(
                onBlogClick = { blogId ->
                    navController.navigate(
                        AppRoute.BlogDetail(blogId)
                    )
                },
            )
        }

        composable<AppRoute.BlogDetail> { backStackEntry ->

            val route: AppRoute.BlogDetail =
                backStackEntry.toRoute()

            BlogDetailScreen(
                blogId = route.blogId,
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
```

COMMENT:
- `composable<AppRoute.X> { ... }` → reified type parameter,
  compiler tahu route-nya
- `backStackEntry.toRoute<AppRoute.BlogDetail>()` →
  extract route + param yang type-safe
- `navController.navigate(AppRoute.BlogDetail(123))` →
  type-safe, autocomplete, tidak ada typo string

---

## 4. Wire ke App.kt

```kotlin
@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}
```

Selesai — tidak perlu ada lagi `BlogScreen()` langsung di App,
karena navigation host yang handle.

---

## 5. Trigger Navigation dari Screen

### Dari List → Detail

`BlogScreen` sudah punya `onBlogClick: (Int) -> Unit` parameter.
NavHost kasih callback yang melakukan navigate:

```kotlin
composable<AppRoute.BlogList> {
    BlogScreen(
        onBlogClick = { blogId ->
            navController.navigate(
                AppRoute.BlogDetail(blogId)
            )
        },
    )
}
```

### Dari Detail → Back

`BlogDetailScreen` punya `onBack: () -> Unit` parameter.
NavHost kasih callback yang pop:

```kotlin
composable<AppRoute.BlogDetail> { backStackEntry ->
    val route: AppRoute.BlogDetail = backStackEntry.toRoute()
    BlogDetailScreen(
        blogId = route.blogId,
        onBack = { navController.popBackStack() },
    )
}
```

COMMENT:
- Screen **TIDAK** punya akses ke `navController` langsung
- Selalu pakai callback (`onClick`, `onBack`) yang
  di-inject dari NavHost
- Pattern ini bikin Screen **reusable** + testable

---

## 6. Detail ViewModel Pattern

### Cara 1: blogId via Method Parameter (Recommended - Simplest)

```kotlin
class BlogDetailViewModel(
    private val getBlogByIdUseCase: GetBlogByIdUseCase,
) : ViewModel() {

    private val _state =
        MutableStateFlow<BlogDetailState>(BlogDetailState.Initial)

    val state = _state.asStateFlow()

    fun onIntent(intent: BlogDetailIntent) {

        when (intent) {

            is BlogDetailIntent.LoadBlog ->
                loadBlog(intent.id)
            // ...
        }
    }
}
```

Screen:

```kotlin
@Composable
fun BlogDetailScreen(
    blogId: Int,
    viewModel: BlogDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
) {

    // Trigger load saat blogId berubah
    LaunchedEffect(blogId) {
        viewModel.onIntent(BlogDetailIntent.LoadBlog(blogId))
    }

    // ...
}
```

COMMENT:
- ViewModel tidak tahu blogId di constructor
- Screen yang trigger via `LaunchedEffect(blogId)`
- `LaunchedEffect(blogId)` re-launch kalau blogId berubah
  (cocok untuk navigation ke detail blog yang berbeda)
- Tidak butuh `parametersOf` di Koin
- Bisa pakai `viewModelOf(::BlogDetailViewModel)` biasa

### Cara 2: blogId via Constructor + Koin Parameters

Kalau mau immutable injection, pakai Koin parameters:

```kotlin
class BlogDetailViewModel(
    private val blogId: Int,
    private val getBlogByIdUseCase: GetBlogByIdUseCase,
) : ViewModel() {

    init { loadBlog() }
}
```

Koin module:

```kotlin
import org.koin.core.module.dsl.viewModel

viewModel { (blogId: Int) ->
    BlogDetailViewModel(blogId, get())
}
```

Screen:

```kotlin
import org.koin.core.parameter.parametersOf

val viewModel = koinViewModel<BlogDetailViewModel>(
    parameters = { parametersOf(blogId) }
)
```

COMMENT:
- Cara 2 lebih strict (blogId immutable)
- Tapi lebih verbose & kompleks
- Untuk learning, Cara 1 lebih recommended

---

## 7. Implementasi Lengkap Detail Screen (MVI)

`features/blogs/presentations/BlogDetailState.kt`

```kotlin
sealed interface BlogDetailState {

    data object Initial : BlogDetailState

    data object Loading : BlogDetailState

    data class Success(
        val blog: BlogModel,
    ) : BlogDetailState

    data class Failure(
        val message: String,
    ) : BlogDetailState
}
```

`features/blogs/presentations/BlogDetailIntent.kt`

```kotlin
sealed class BlogDetailIntent {

    data class LoadBlog(val id: Int) : BlogDetailIntent()

    data object Retry : BlogDetailIntent()
}
```

`features/blogs/presentations/BlogDetailEffect.kt`

```kotlin
sealed class BlogDetailEffect {

    data class ShowSnackbar(val message: String) : BlogDetailEffect()
}
```

`features/blogs/presentations/BlogDetailViewModel.kt`

```kotlin
class BlogDetailViewModel(
    private val getBlogByIdUseCase: GetBlogByIdUseCase,
) : ViewModel() {

    private val _state =
        MutableStateFlow<BlogDetailState>(BlogDetailState.Initial)

    val state = _state.asStateFlow()

    private val _effect = Channel<BlogDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentBlogId: Int? = null

    fun onIntent(intent: BlogDetailIntent) {

        when (intent) {

            is BlogDetailIntent.LoadBlog -> {
                currentBlogId = intent.id
                loadBlog(intent.id)
            }

            BlogDetailIntent.Retry -> {
                currentBlogId?.let { loadBlog(it) }
            }
        }
    }

    private fun loadBlog(id: Int) {

        viewModelScope.launch {

            _state.value = BlogDetailState.Loading

            when (val result = getBlogByIdUseCase(id)) {

                is Result.Success ->
                    _state.value = BlogDetailState.Success(
                        blog = result.data
                    )

                is Result.Error -> {

                    val message = result.failure.message

                    _state.value = BlogDetailState.Failure(message)

                    _effect.send(
                        BlogDetailEffect.ShowSnackbar(message)
                    )
                }
            }
        }
    }
}
```

COMMENT:
- `currentBlogId` di-simpan supaya `Retry` tahu blog mana yang harus di-load ulang
- Same MVI pattern dengan list screen (Intent, State, Effect, ViewModel)
- Pakai `_state.value = ...` karena state sealed (bukan data class)

`features/blogs/presentations/BlogDetailScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: Int,
    viewModel: BlogDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(blogId) {
        viewModel.onIntent(BlogDetailIntent.LoadBlog(blogId))
    }

    LaunchedEffect(Unit) {

        viewModel.effect.collect { effect ->

            when (effect) {

                is BlogDetailEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blog Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            when (val s = state) {

                BlogDetailState.Initial,
                BlogDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is BlogDetailState.Success -> {
                    BlogDetailContent(blog = s.blog)
                }

                is BlogDetailState.Failure -> {
                    ErrorView(
                        message = s.message,
                        onRetry = {
                            viewModel.onIntent(BlogDetailIntent.Retry)
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
```

---

## 8. Register di Koin Module

`features/blogs/di/BlogModule.kt`

```kotlin
val blogModule = module {

    // Datasource
    singleOf(::BlogsApiImpl) {
        bind<BlogsApi>()
    }

    // Repository
    singleOf(::BlogRepositoryImpl) {
        bind<BlogRepository>()
    }

    // UseCases
    factoryOf(::GetBlogsUseCase)
    factoryOf(::GetBlogByIdUseCase)

    // ViewModels
    viewModelOf(::BlogViewModel)
    viewModelOf(::BlogDetailViewModel)
}
```

---

## 9. Alur Lengkap Navigasi

```txt
1. User buka app
   ↓
2. AppNavigation NavHost render
   ↓
3. startDestination = BlogList → BlogScreen()
   ↓
4. BlogViewModel init → loadBlogs() → State.Success(blogs)
   ↓
5. User tap blog item
   ↓
6. BlogScreen → onBlogClick(blog.id)
   ↓
7. NavHost callback → navController.navigate(AppRoute.BlogDetail(id))
   ↓
8. Compose Navigation push entry baru
   ↓
9. composable<BlogDetail> render → BlogDetailScreen(blogId = id)
   ↓
10. BlogDetailScreen LaunchedEffect(blogId) → trigger LoadBlog intent
    ↓
11. BlogDetailViewModel → loadBlog(id) → State.Success(blog)
    ↓
12. User tap "← Back"
    ↓
13. BlogDetailScreen → onBack()
    ↓
14. NavHost callback → navController.popBackStack()
    ↓
15. Kembali ke BlogScreen (state masih terjaga di ViewModel)
```

---

## 10. Tips & Patterns

### Pass Multiple Params

```kotlin
@Serializable
data class UserProfile(
    val userId: Int,
    val tab: String = "info",
) : AppRoute
```

Navigate:

```kotlin
navController.navigate(
    AppRoute.UserProfile(userId = 123, tab = "posts")
)
```

### Pop sampai Route Tertentu

```kotlin
navController.popBackStack(
    route = AppRoute.BlogList,
    inclusive = false,
)
```

### Clear Back Stack (Login Flow)

```kotlin
// Setelah login berhasil, ke home + clear back stack
navController.navigate(AppRoute.Home) {
    popUpTo(AppRoute.Login) { inclusive = true }
}
```

### Pass Complex Object

Untuk object kompleks (selain primitive/Serializable), pakai
**ID saja** + fetch ulang di destination. Jangan pass full
object lewat route — itu anti-pattern.

```kotlin
// ❌ JANGAN — antar route pass full Blog object
data class BlogDetail(val blog: Blog) : AppRoute

// ✅ DO — pass ID, fetch ulang di detail
data class BlogDetail(val blogId: Int) : AppRoute
```

---

## 11. Cheat Sheet

| Aksi | Code |
|---|---|
| Navigate ke route | `navController.navigate(AppRoute.X)` |
| Pop back | `navController.popBackStack()` |
| Pop sampai route | `navController.popBackStack(AppRoute.X, false)` |
| Replace current | `navController.navigate(X) { popUpTo(current) { inclusive = true } }` |
| Current route | `navController.currentDestination?.route` |
| Get backStackEntry | `composable<X> { backStackEntry -> ... }` |
| Extract route param | `backStackEntry.toRoute<AppRoute.X>()` |

---

## 12. Folder Structure setelah Punya Navigation

```txt
shared/src/commonMain/kotlin/.../
├── core/
│   ├── navigation/
│   │   └── AppNavigation.kt    ← Routes + NavHost
│   ├── network/
│   ├── util/
│   └── modules/
│
└── features/
    └── blogs/
        ├── data/
        ├── domain/
        ├── presentations/
        │   ├── BlogIntent.kt
        │   ├── BlogState.kt
        │   ├── BlogEffect.kt
        │   ├── BlogViewModel.kt
        │   ├── BlogScreen.kt
        │   ├── BlogDetailIntent.kt
        │   ├── BlogDetailState.kt
        │   ├── BlogDetailEffect.kt
        │   ├── BlogDetailViewModel.kt
        │   └── BlogDetailScreen.kt
        └── di/
            └── BlogModule.kt
```

---

## Navigasi Docs

- [⬅ 07 — Testing](07-testing.md)
- [⬅ 01 — Overview](01-overview.md) (back to start)
