# 05 — Presentation Layer (MVI)

Layer ini bertanggung jawab atas UI dan state
management. KMP pakai **MVI (Model-View-Intent)**
pattern dengan **sealed interface State** — paling
idiomatic Kotlin: type-safe + smart cast + impossible
to have invalid state.

---

## Konsep MVI

| Konsep | Tujuan | Equivalent Flutter |
|---|---|---|
| **Intent** | Aksi user (sealed class) | Bloc Event |
| **State** | UI state saat ini (sealed interface) | Bloc State (Initial/Loading/Loaded/Failure) |
| **Effect** | One-shot event (navigation, snackbar) | Bloc Listener |
| **ViewModel** | Container business logic | Cubit / Bloc |
| **Reducer** | Update state berdasarkan intent | `on<Event>((event, emit) {...})` |

---

## Folder Structure: Subfolder per Screen

Tiap screen punya subfolder sendiri, dengan **5 file
terpisah**: Intent, State, Effect, ViewModel, Screen.

```txt
features/blogs/presentations/
├── list/
│   ├── BlogIntent.kt
│   ├── BlogState.kt
│   ├── BlogEffect.kt
│   ├── BlogViewModel.kt
│   └── BlogScreen.kt
└── detail/
    ├── BlogDetailIntent.kt
    ├── BlogDetailState.kt
    ├── BlogDetailEffect.kt
    ├── BlogDetailViewModel.kt
    └── BlogDetailScreen.kt
```

COMMENT:
- 1 subfolder per screen → grouping yang jelas
- 5 file per screen → konsep MVI terpisah dengan tegas
- Mirip dengan pattern di Flutter:
  `presentation/blog_list/` dengan `bloc.dart`,
  `event.dart`, `state.dart` terpisah
- Future-proof: gak perlu refactor saat sealed class tumbuh
  besar (misal form login dengan banyak field/validation)

### Trade-off

| Aspek | File Terpisah ✅ | Bundle (Contract.kt) |
|---|---|---|
| Mudah cari per concept | ✅ | ⚠️ Harus scroll |
| File count | 5 per screen | 3 per screen |
| Scalable saat sealed tumbuh besar | ✅ | ❌ Refactor lagi |
| Cocok untuk simple screen | ⚠️ Sedikit overkill | ✅ |

**Kapan pilih bundle (Contract.kt)?** Kalau yakin
screen-nya tetap simple (Intent <5, State <5 variant,
Effect <3). Tapi kalau ragu-ragu, **pisah** lebih aman.

### Alternatif: Region Folding

Kalau mau bundle tapi tetap gampang di-scan, pakai
`//region` `//endregion` di IDE:

```kotlin
//region Intent
sealed class BlogIntent { ... }
//endregion

//region State
sealed interface BlogState { ... }
//endregion

//region Effect
sealed class BlogEffect { ... }
//endregion
```

Click triangle di sebelah `//region` untuk collapse.

---

## Alur MVI

```txt
Compose UI
    ↓ onIntent(Intent.Refresh)
ViewModel.onIntent(...)
    ↓ viewModelScope.launch
UseCase()
    ↓ Result<T>
_state.value = BlogState.Success(...)
    ↓ StateFlow emit
Compose recompose (with smart cast)
```

---

## 1. Intent (Bloc Event Equivalent)

### Flutter Bloc Event

```dart
abstract class LoginEvent {}
```

### KMP MVI Intent

`features/blogs/presentations/BlogIntent.kt`

```kotlin
sealed class BlogIntent {

    data object LoadBlogs : BlogIntent()

    data object Refresh : BlogIntent()

    data class BlogClicked(val id: Int) : BlogIntent()
}
```

COMMENT:
- Intent di MVI ≈ Event di Bloc
- `sealed class` exhaustive, compiler enforce
  handle semua case di `when`
- `data object` untuk intent tanpa parameter
- `data class` untuk intent dengan parameter

---

## 2. State (Sealed Interface — Kotlin Idiomatic)

Di Flutter biasanya kita pakai `data class` dengan
boolean flag (`isLoading`, `isError`) atau pakai
status enum (`BlogStatus.loading`). Di Kotlin yang
**paling idiomatic** adalah **sealed interface**.

### Kenapa Sealed Interface?

| Pendekatan | Type Safety | Invalid State? |
|---|---|---|
| `data class BlogState(isLoading, blogs, error)` | ⚠️ Lemah | ✅ Bisa: `isLoading=true, error="..."` 🤔 |
| `enum BlogStatus { initial, loading, ... }` | ⚠️ Status & data terpisah | ⚠️ Bisa: `status=LOADED, blogs=[]` |
| **`sealed interface`** | ✅ Strict | ❌ **Tidak mungkin** invalid |

### Implementation

`features/blogs/presentations/BlogState.kt`

```kotlin
sealed interface BlogState {

    data object Initial : BlogState

    data object Loading : BlogState

    data class Success(
        val blogs: List<BlogModel>,
        val isRefreshing: Boolean = false,
    ) : BlogState

    data class Failure(
        val message: String,
    ) : BlogState
}
```

COMMENT:
- `sealed interface` (Kotlin 1.5+) lebih fleksibel
  dari `sealed class` (bisa di-implement multiple)
- `data object` = singleton untuk state tanpa data
- Data **menyatu dengan state-nya**:
  - `blogs` hanya ada di `Success`
  - `message` hanya ada di `Failure`
- `isRefreshing` di-nest dalam `Success` supaya
  pull-to-refresh tetap menampilkan data lama

### Smart Cast Magic

Compiler otomatis cast state saat ada `is BlogState.X`
check:

```kotlin
when (val s = state) {

    is BlogState.Success ->
        Text(s.blogs.size.toString())   // ← .blogs aman, smart cast

    is BlogState.Failure ->
        Text(s.message)                  // ← .message aman, smart cast

    else -> {}
}
```

---

## 3. Effect (One-shot Side Effect)

**Bedanya dengan State:**
- **State** persistent (di-collect terus oleh UI)
- **Effect** one-shot (hanya dikirim sekali, gak
  re-emit saat recomposition)

Kapan pakai Effect:
- Navigation (`navigate to detail`)
- Snackbar / Toast
- Show dialog confirmation
- External action (open browser, share, dll)

### Implementation

`features/blogs/presentations/BlogEffect.kt`

```kotlin
sealed class BlogEffect {

    data class ShowSnackbar(val message: String) : BlogEffect()

    data class NavigateToDetail(val blogId: Int) : BlogEffect()
}
```

COMMENT:
- Effect ≈ Bloc Listener atau navigation event
- Pakai `Channel` (bukan `StateFlow`) supaya
  gak re-emit saat recompose
- Subscribe pakai `LaunchedEffect`

---

## 4. ViewModel (Cubit Equivalent)

### Flutter Cubit

```dart
class LoginCubit extends Cubit<LoginState> {
  Future<void> submit() async {
    emit(state.copyWith(isLoading: true));
  }
}
```

### KMP MVI ViewModel

`features/blogs/presentations/BlogViewModel.kt`

```kotlin
class BlogViewModel(
    private val getBlogsUseCase: GetBlogsUseCase,
) : ViewModel() {

    // State (persistent)
    private val _state =
        MutableStateFlow<BlogState>(BlogState.Initial)

    val state = _state.asStateFlow()

    // Effect (one-shot)
    private val _effect = Channel<BlogEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(BlogIntent.LoadBlogs)
    }

    fun onIntent(intent: BlogIntent) {

        when (intent) {

            BlogIntent.LoadBlogs ->
                loadBlogs(isRefresh = false)

            BlogIntent.Refresh ->
                loadBlogs(isRefresh = true)

            is BlogIntent.BlogClicked ->
                viewModelScope.launch {
                    _effect.send(
                        BlogEffect.NavigateToDetail(intent.id)
                    )
                }
        }
    }

    private fun loadBlogs(isRefresh: Boolean) {

        viewModelScope.launch {

            val current = _state.value

            // Set loading state berdasarkan apakah ini refresh atau load awal
            _state.value = when {

                isRefresh && current is BlogState.Success ->
                    current.copy(isRefreshing = true)

                else -> BlogState.Loading
            }

            // Execute use case
            when (val result = getBlogsUseCase()) {

                is Result.Success ->
                    _state.value = BlogState.Success(
                        blogs = result.data,
                        isRefreshing = false,
                    )

                is Result.Error -> {

                    val message = result.failure.message

                    // Kalau refresh gagal tapi data lama ada → tetap tampilkan
                    // Kalau initial load gagal → error screen
                    if (current is BlogState.Success) {
                        _state.value = current.copy(
                            isRefreshing = false
                        )
                        _effect.send(
                            BlogEffect.ShowSnackbar(message)
                        )
                    } else {
                        _state.value = BlogState.Failure(message)
                    }
                }
            }
        }
    }
}
```

COMMENT:
- `MutableStateFlow<BlogState>(BlogState.Initial)` —
  type parameter eksplisit dibutuhkan supaya
  bisa assign subtype lain (`Loading`, `Success`, dll)
- `_state.value = ...` (bukan `update {}`) karena
  state-nya **bukan data class** — kita ganti
  objeknya, bukan update field
- `current is BlogState.Success` → smart cast,
  `current.copy(...)` available
- Pattern "refresh tetap tampilkan data lama" =
  modifikasi `current` dengan `copy(isRefreshing = true)`

---

## 5. Compose Screen (UI Layer)

### Flutter

```dart
BlocBuilder<LoginCubit, LoginState>(
  builder: (_, state) {
    return Text(state.email);
  },
)
```

### KMP Compose

`features/blogs/presentations/BlogScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogScreen(
    viewModel: BlogViewModel = koinViewModel(),
    onBlogClick: (Int) -> Unit = {},
) {

    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-shot effects
    LaunchedEffect(Unit) {

        viewModel.effect.collect { effect ->

            when (effect) {

                is BlogEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)

                is BlogEffect.NavigateToDetail ->
                    onBlogClick(effect.blogId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blogs") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.onIntent(BlogIntent.Refresh)
                        },
                    ) {
                        Text("Refresh")
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

            // when exhaustive + smart cast
            when (val s = state) {

                BlogState.Initial,
                BlogState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is BlogState.Success -> {
                    BlogList(
                        blogs = s.blogs,                  // ← smart cast
                        isRefreshing = s.isRefreshing,    // ← smart cast
                        onBlogClick = { id ->
                            viewModel.onIntent(BlogIntent.BlogClicked(id))
                        },
                    )
                }

                is BlogState.Failure -> {
                    ErrorView(
                        message = s.message,              // ← smart cast
                        onRetry = {
                            viewModel.onIntent(BlogIntent.LoadBlogs)
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
```

COMMENT:
- `collectAsState()` ≈ BlocBuilder
- `LaunchedEffect(Unit)` ≈ initState (sekali jalan)
- `when (val s = state)` → assign ke local `s`
  untuk smart cast
- `BlogState.Initial, BlogState.Loading ->` =
  match multiple case (mirip Dart switch)
- Compose otomatis recompose saat state berubah

### Sub-components

```kotlin
@Composable
private fun BlogList(
    blogs: List<BlogModel>,
    isRefreshing: Boolean,
    onBlogClick: (Int) -> Unit,
) {

    Column {

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            items(
                items = blogs,
                key = { it.id },
            ) { blog ->

                BlogItem(
                    blog = blog,
                    onClick = { onBlogClick(blog.id) },
                )
            }
        }
    }
}

@Composable
private fun BlogItem(
    blog: BlogModel,
    onClick: () -> Unit,
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            Text(
                text = blog.title,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = blog.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(onClick = onRetry) {
            Text("Coba Lagi")
        }
    }
}
```

---

## 6. UI States yang Dihandle

| State | Kondisi | UI Tampilan |
|---|---|---|
| `Initial` | Awal sebelum init load jalan | `CircularProgressIndicator` |
| `Loading` | First load berjalan | `CircularProgressIndicator` |
| `Success(isRefreshing=false)` | Data ada | List blog |
| `Success(isRefreshing=true)` | Refresh berjalan, data lama tetap tampil | `LinearProgressIndicator` di atas list |
| `Failure` | First load gagal | `ErrorView` dengan tombol "Coba Lagi" |
| (Refresh gagal saat Success) | Data lama tetap tampil | Snackbar (via Effect) |

---

## 7. Wire ke App.kt

```kotlin
@Composable
fun App() {
    MaterialTheme {
        BlogScreen(
            onBlogClick = { blogId ->
                // TODO: handle navigation ke detail
                println("Clicked blog: $blogId")
            }
        )
    }
}
```

Import yang dibutuhkan:

```kotlin
import org.example.project
    .features.blogs.presentations.BlogScreen
```

---

## 8. Register di Koin Module

`features/blogs/di/BlogModule.kt`

```kotlin
import org.koin.core.module.dsl.viewModelOf

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

    // ViewModels
    viewModelOf(::BlogViewModel)
}
```

COMMENT:
- `viewModelOf` dari Koin 4+ (di `koin-core`)
- ViewModel scope = factory, baru per instance
- `koinViewModel()` di Compose auto-detect

Detail lengkap modular DI: lihat
[06 — Dependency Injection](06-dependency-injection.md).

---

## 9. Shared UI Components

Saat kamu punya 2+ screen yang pakai komponen yang sama
(misal `ErrorView`, `LoadingIndicator`, `EmptyView`),
extract ke folder `core/ui/components/`. Mirip pattern
di Flutter di mana kita simpan widget reusable di
`lib/core/widgets/`.

### Struktur

```txt
shared/src/commonMain/kotlin/.../core/
└── ui/
    └── components/
        ├── LoadingIndicator.kt
        ├── ErrorView.kt
        └── EmptyView.kt
```

---

### LoadingIndicator

`core/ui/components/LoadingIndicator.kt`

```kotlin
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
```

COMMENT:
- **Self-centering** — pakai `Box` dengan `contentAlignment`
- `fillMaxSize()` supaya mengisi parent sepenuhnya
- Call site cukup `LoadingIndicator()` — clean

### Pakai di Screen

```kotlin
when (val s = state) {
    BlogState.Initial,
    BlogState.Loading -> LoadingIndicator()
    // ...
}
```

---

### ErrorView

`core/ui/components/ErrorView.kt`

```kotlin
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Coba Lagi",
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(onClick = onRetry) {
                Text(retryLabel)
            }
        }
    }
}
```

COMMENT:
- Self-centering juga (mirip `LoadingIndicator`)
- `retryLabel` punya default value untuk i18n
- `modifier: Modifier = Modifier` = mandatory parameter
  untuk composable yang bisa di-style oleh parent
  (Compose best practice)

### Pakai di Screen

```kotlin
is BlogState.Failure -> {
    ErrorView(
        message = s.message,
        onRetry = {
            viewModel.onIntent(BlogIntent.LoadBlogs)
        },
    )
}
```

COMMENT:
- TIDAK perlu `.align(Alignment.Center)` lagi —
  ErrorView sudah self-centering

---

### EmptyView

`core/ui/components/EmptyView.kt`

```kotlin
@Composable
fun EmptyView(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )

            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
```

COMMENT:
- Action **optional** — kalau gak butuh tombol,
  cukup pass `message` saja
- Pattern `actionLabel: String? = null, onAction: (() -> Unit)? = null`
  = sinkron, both `null` atau both ada

### Pakai di Screen

```kotlin
is BlogState.Success -> {

    if (s.blogs.isEmpty()) {
        EmptyView(
            message = "Belum ada blog",
            actionLabel = "Reload",
            onAction = {
                viewModel.onIntent(BlogIntent.Refresh)
            },
        )
    } else {
        BlogList(...)
    }
}
```

Atau tanpa action:

```kotlin
EmptyView(message = "Wishlist kosong")
```

---

### Mapping Pattern Flutter → Compose

| Flutter Widget | Compose Component |
|---|---|
| `Center(child: CircularProgressIndicator())` | `LoadingIndicator()` |
| Custom error widget dengan retry | `ErrorView(message, onRetry)` |
| Custom empty state widget | `EmptyView(message, actionLabel, onAction)` |

---

### Aturan untuk Komponen Shared

| Hal | Aturan |
|---|---|
| Lokasi | `core/ui/components/` (cross-feature) atau `features/X/ui/components/` (feature-specific) |
| Visibility | `public` (default), jangan `private` |
| Dependency | **TIDAK BOLEH** depend ke ViewModel atau Repository |
| State | Stateless (terima state via parameter, expose action via lambda) |
| Modifier | Selalu terima `modifier: Modifier = Modifier` parameter |
| Default values | Gunakan untuk customization optional (label, color, dll) |
| Layout | Self-centering dengan `Box + fillMaxSize + contentAlignment.Center` |

### Kapan Extract ke Shared?

✅ **Extract** kalau:
- Dipakai di 2+ screen (DRY)
- Style/behavior sama persis
- Cocok jadi reusable component (Error, Loading, Empty)

❌ **JANGAN extract** kalau:
- Cuma dipakai 1 screen → biarin private
- Pemakaian sangat berbeda style/behavior antar screen
- Logic tightly-coupled dengan ViewModel feature

---

## 10. Mapping Konsep MVI

| Bloc / Cubit (Flutter) | MVI (KMP) |
|---|---|
| `Bloc<Event, State>` | `class BlogViewModel : ViewModel()` |
| `Event` | `Intent` (sealed class) |
| `State` (sealed/data class) | `State` (sealed interface) |
| `emit(state.copyWith(...))` | `_state.value = current.copy(...)` |
| `emit(NewState())` | `_state.value = BlogState.X` |
| `BlocBuilder<B, S>` | `collectAsState()` + `when` |
| `BlocListener<B, S>` | `LaunchedEffect { effect.collect { } }` |
| `on<Event>((e, emit) { })` | `when (intent) { ... }` |
| `context.read<Bloc>()` | `koinViewModel()` / `koinInject()` |

---

## 11. Cheat Sheet: Sealed Interface vs Data Class

| Pertanyaan | Sealed Interface | Data Class + Flags |
|---|---|---|
| Type safety strict? | ✅ Ya | ⚠️ Bisa invalid state |
| Smart cast otomatis? | ✅ Ya | ❌ Tidak |
| Cocok untuk simple loading state? | ✅ Sangat | ⚠️ Overkill |
| Cocok untuk form multi-field? | ⚠️ Verbose | ✅ Lebih praktis |
| Cocok untuk pull-to-refresh? | ✅ (dengan flag di Success) | ✅ Lebih natural |
| Cocok untuk pagination? | ⚠️ Kompleks | ✅ Lebih praktis |
| Idiomatic Kotlin? | ⭐ Most idiomatic | ⚠️ Acceptable |
| Update syntax | `_state.value = X` | `_state.update { it.copy(...) }` |

**Rule of thumb:**
- **Sealed interface** → screen dengan fase yang jelas
  (Loading → Loaded/Error)
- **Data class** → form multi-field, pagination,
  pull-to-refresh kompleks

---

## 12. Cheat Sheet: State vs Effect

| Pertanyaan | State | Effect |
|---|---|---|
| Persisten lintas recompose? | ✅ Ya | ❌ Tidak (one-shot) |
| Dapat di-restore setelah config change? | ✅ Ya | ❌ Tidak |
| Cocok untuk loading/error/data? | ✅ Ya | ❌ Tidak |
| Cocok untuk snackbar/navigation? | ❌ Tidak | ✅ Ya |
| Type | `StateFlow<SealedInterface>` | `Channel` + `receiveAsFlow` |
| Di-collect dengan | `collectAsState()` + `when` | `LaunchedEffect { collect { } }` |

---

## Navigasi Docs

- [⬅ 04 — Domain Layer](04-domain-layer.md)
- [➡ 06 — Dependency Injection](06-dependency-injection.md)
- [07 — Testing](07-testing.md)
