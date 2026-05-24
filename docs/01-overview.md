# 01 — Overview

## Tentang Project Ini

Project ini dibuat untuk membantu **Flutter developer**
memahami **Kotlin Multiplatform (KMP)** menggunakan
pendekatan yang sangat mirip dengan:

- flutter_bloc
- clean architecture
- get_it
- injectable
- dartz
- dio

## Stack yang Digunakan

- KMP
- Compose Multiplatform
- MVI
- Koin
- Ktor
- StateFlow

---

## Flutter vs KMP Mapping

| Flutter | KMP |
|---|---|
| flutter_bloc | MVI + StateFlow |
| Cubit | ViewModel |
| emit() | _state.update() |
| BlocBuilder | collectAsStateWithLifecycle() |
| get_it | Koin |
| injectable | Koin module |
| dio | Ktor |
| dartz Either | Result sealed class |
| Stream | Flow |
| Future | suspend function |

---

## Flutter Analogy

Flutter:

```dart
UI
 ↓
Bloc/Cubit
 ↓
UseCase
 ↓
Repository
 ↓
Datasource
```

KMP:

```txt
UI
 ↓
ViewModel (MVI)
 ↓
UseCase
 ↓
Repository
 ↓
Ktor API
```

---

## Project Structure

```txt
shared/
├── core/
│   ├── navigation/             ← NavHost + AppRoute
│   ├── network/
│   ├── ui/
│   │   └── components/         ← shared composables (ErrorView, dll)
│   ├── util/
│   ├── usecase/
│   └── modules/
│
└── features/
    └── blogs/
        ├── data/
        │   ├── datasources/api/
        │   ├── dto/
        │   ├── mapper/
        │   └── repository/
        ├── domain/
        │   ├── model/
        │   ├── repository/
        │   └── usecases/
        ├── presentations/
        │   ├── list/
        │   │   ├── BlogIntent.kt
        │   │   ├── BlogState.kt
        │   │   ├── BlogEffect.kt
        │   │   ├── BlogViewModel.kt
        │   │   └── BlogScreen.kt
        │   └── detail/
        │       ├── BlogDetailIntent.kt
        │       ├── BlogDetailState.kt
        │       ├── BlogDetailEffect.kt
        │       ├── BlogDetailViewModel.kt
        │       └── BlogDetailScreen.kt
        └── di/
            └── BlogModule.kt
```

---

## Final Mental Model

```txt
Flutter Bloc
↓
Event
↓
Cubit
↓
emit()
↓
UI rebuild
```

```txt
KMP MVI
↓
Intent
↓
ViewModel
↓
StateFlow update
↓
Compose recompose
```

---

## Recommendation For You

Karena kamu sudah familiar dengan:

- flutter_bloc
- clean architecture
- dartz
- get_it
- reactive architecture

Maka approach terbaik di KMP:

```txt
Compose
+ MVI
+ StateFlow
+ Koin
+ Ktor
+ Clean Architecture
```

Ini akan terasa paling natural untuk transisi
dari Flutter.

---

## Navigasi Docs

- [02 — Setup Ktor & Koin](02-setup-ktor-koin.md)
- [03 — Data Layer](03-data-layer.md)
- [04 — Domain Layer](04-domain-layer.md)
- [05 — Presentation Layer (MVI)](05-presentation-layer-mvi.md)
- [06 — Dependency Injection](06-dependency-injection.md)
- [07 — Testing](07-testing.md)
- [08 — Navigation](08-navigation.md)
