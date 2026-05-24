# KMP Architecture For Flutter Developer

Repo ini berisi catatan belajar **Kotlin Multiplatform
(KMP)** dari sudut pandang **Flutter developer**. Pendekatan
arsitektur, naming, dan polanya sengaja dibikin sangat
mirip dengan stack Flutter populer (flutter_bloc, dartz,
get_it, injectable, dio).

---

## 📖 Documentation

Semua dokumentasi dipisah per layer agar mudah dibaca:

| # | Topik | Isi |
|---|---|---|
| 01 | [Overview](docs/01-overview.md) | Intro, Flutter vs KMP mapping, mental model |
| 02 | [Setup Ktor & Koin](docs/02-setup-ktor-koin.md) | Setup step by step, platform engines, expect/actual |
| 03 | [Data Layer](docs/03-data-layer.md) | DTO, Mapper, API, Repository Impl, Auth, Custom Error |
| 04 | [Domain Layer](docs/04-domain-layer.md) | Result, Failure, Repository interface, UseCase |
| 05 | [Presentation Layer (MVI)](docs/05-presentation-layer-mvi.md) | Intent, State, Effect, ViewModel, Compose Screen |
| 06 | [Dependency Injection](docs/06-dependency-injection.md) | Koin modules, per-feature pattern, Constructor DSL |
| 07 | [Testing](docs/07-testing.md) | MockK, Turbine, per-layer testing |
| 08 | [Navigation](docs/08-navigation.md) | Compose Navigation type-safe routes, list ↔ detail screen |

---

## 🚀 Quick Stack Reference

```txt
KMP
+ Compose Multiplatform
+ MVI
+ Koin
+ Ktor
+ StateFlow
+ Clean Architecture
```

---

## 🔁 Flutter vs KMP — Cheat Sheet

| Flutter | KMP |
|---|---|
| flutter_bloc | MVI + StateFlow |
| Cubit | ViewModel |
| emit() | _state.update() |
| BlocBuilder | collectAsState() |
| get_it | Koin |
| injectable | Koin module |
| dio | Ktor |
| dartz Either | Result sealed class |
| Stream | Flow |
| Future | suspend function |
| flutter_test + mockito | kotlin.test + MockK |
| bloc_test | Turbine |

---

## 🗂️ Project Structure

```txt
shared/src/commonMain/kotlin/.../
├── core/
│   ├── navigation/             ← AppRoute + NavHost
│   ├── network/                ← HttpClient factory + engine per-platform
│   ├── ui/
│   │   └── components/         ← shared composables (ErrorView, dll)
│   ├── util/                   ← Result, Failure sealed class
│   ├── usecase/                ← UseCase base interface
│   └── modules/                ← AppModules + NetworkModule (Koin)
│
└── features/
    └── blogs/
        ├── data/
        │   ├── datasources/api/   ← BlogsApi (Ktor calls)
        │   ├── dto/               ← BlogDto (@Serializable)
        │   ├── mapper/            ← BlogDto.toDomain()
        │   └── repository/        ← BlogRepositoryImpl
        ├── domain/
        │   ├── model/             ← BlogModel (entity)
        │   ├── repository/        ← BlogRepository (interface)
        │   └── usecases/          ← GetBlogsUseCase, GetBlogByIdUseCase
        ├── presentations/
        │   ├── list/              ← MVI list: Intent + State + Effect + VM + Screen
        │   └── detail/            ← MVI detail: Intent + State + Effect + VM + Screen
        └── di/
            └── BlogModule.kt
```

---

## 🎯 Untuk Flutter Developer

Kalau kamu sudah familiar dengan:

- `flutter_bloc` + Cubit
- Clean Architecture (data/domain/presentation)
- `dartz Either<Failure, T>`
- `get_it` + `injectable`
- `dio` interceptors

Maka transisi ke stack ini akan terasa **sangat natural**.
Mulai dari [01 — Overview](docs/01-overview.md).

---

## ▶️ Cara Pakai

1. Baca **dari atas ke bawah** dimulai dari
   [01 — Overview](docs/01-overview.md)
2. Setiap docs file memiliki link navigasi
   ke prev/next di bagian bawah
3. Setiap section punya **Flutter equivalent**
   untuk konteks
4. Section yang ada `COMMENT:` adalah penjelasan
   tambahan, bisa di-skip kalau sudah paham

---

## 🤝 Contributing

Catatan ini berkembang seiring belajar. Kalau ada
pattern yang lebih baik atau koreksi, silakan
update file docs yang sesuai.
