package org.example.project.core.modules

import io.ktor.client.HttpClient
import org.example.project.features.blogs.data.datasources.api.BlogsApi
import org.example.project.features.blogs.data.datasources.api.BlogsApiImpl
import org.example.project.features.blogs.data.repository.BlogRepositoryImpl
import org.example.project.features.blogs.domain.repository.BlogRepository
import org.example.project.features.blogs.domain.usecases.GetBlogByIdUseCase
import org.example.project.features.blogs.domain.usecases.GetBlogsUseCase
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Tests untuk verifikasi Koin DI graph.
 *
 * Tujuan:
 * - Pastikan semua binding di appModules bisa diresolve
 * - Pastikan interface bound ke implementasi yang benar
 * - Tangkap error DI saat test (bukan saat runtime)
 *
 * Pakai `koinApplication { }` (bukan `startKoin`) supaya
 * gak modifikasi global state Koin antar test.
 */
class AppModulesTest {

    /**
     * Helper untuk bikin Koin instance yang isolated.
     * Tiap test punya Koin sendiri, gak konflik.
     */
    private fun createKoin() = koinApplication {
        modules(appModules)
    }.koin

    // ============================================================
    //  Network Layer
    // ============================================================

    @Test
    fun `HttpClient can be resolved from appModules`() {

        val koin = createKoin()

        // Jika DI graph rusak, baris ini akan throw exception
        val client = koin.get<HttpClient>()

        assertIs<HttpClient>(client)
    }

    // ============================================================
    //  Blog Feature - Data Layer
    // ============================================================

    @Test
    fun `BlogsApi is bound to BlogsApiImpl`() {

        val koin = createKoin()

        val api = koin.get<BlogsApi>()

        // Verifikasi interface bound ke implementasi yang benar
        assertIs<BlogsApiImpl>(api)
    }

    @Test
    fun `BlogRepository is bound to BlogRepositoryImpl`() {

        val koin = createKoin()

        val repository = koin.get<BlogRepository>()

        assertIs<BlogRepositoryImpl>(repository)
    }

    // ============================================================
    //  Blog Feature - Domain Layer
    // ============================================================

    @Test
    fun `GetBlogsUseCase can be resolved with dependencies`() {

        val koin = createKoin()

        // UseCase butuh BlogRepository
        // → BlogRepository butuh BlogsApi
        // → BlogsApi butuh HttpClient
        // Kalau ada yang missing, get() bakal throw
        val useCase = koin.get<GetBlogsUseCase>()

        assertIs<GetBlogsUseCase>(useCase)
    }

    @Test
    fun `GetBlogByIdUseCase can be resolved with dependencies`() {

        val koin = createKoin()

        val useCase = koin.get<GetBlogByIdUseCase>()

        assertIs<GetBlogByIdUseCase>(useCase)
    }

    // ============================================================
    //  Singleton Consistency
    // ============================================================

    @Test
    fun `HttpClient is singleton - same instance returned`() {

        val koin = createKoin()

        val first = koin.get<HttpClient>()
        val second = koin.get<HttpClient>()

        // single { ... } harus return instance yang sama
        assertIs<HttpClient>(first)
        assertIs<HttpClient>(second)
        kotlin.test.assertSame(
            first,
            second,
            "HttpClient harus singleton"
        )
    }

    @Test
    fun `BlogRepository is singleton - same instance returned`() {

        val koin = createKoin()

        val first = koin.get<BlogRepository>()
        val second = koin.get<BlogRepository>()

        kotlin.test.assertSame(
            first,
            second,
            "BlogRepository harus singleton"
        )
    }

    @Test
    fun `GetBlogsUseCase is factory - different instances`() {

        val koin = createKoin()

        val first = koin.get<GetBlogsUseCase>()
        val second = koin.get<GetBlogsUseCase>()

        // factory { ... } harus return instance baru
        kotlin.test.assertNotSame(
            first,
            second,
            "UseCase harus factory (instance baru per resolve)"
        )
    }
}
