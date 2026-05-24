package org.example.project.features.blogs.fakes

import org.example.project.features.blogs.data.datasources.api.BlogsApi
import org.example.project.features.blogs.data.dto.BlogDto

/**
 * Fake implementation dari BlogsApi untuk testing.
 *
 * Pendekatan "Fake" = tulis implementasi manual yang
 * memenuhi interface, dengan property publik untuk
 * mengatur perilaku dari test.
 *
 * Kenapa Fake (bukan MockK)?
 * - MockK gak fully multiplatform (mostly JVM/Android)
 * - Fake jalan di commonTest = Android + iOS + Desktop
 * - Lebih type-safe, gak ada string-based mocking
 * - Lebih pedagogis - jelas perilakunya
 */
class FakeBlogsApi : BlogsApi {

    // Setter untuk response yang akan dikembalikan
    var blogsResponse: List<BlogDto> = emptyList()
    var blogByIdResponse: BlogDto? = null

    // Setter untuk simulasi error
    var shouldThrowOnGetBlogs: Boolean = false
    var shouldThrowOnGetBlogById: Boolean = false
    var thrownException: Exception = RuntimeException("Test exception")

    // Counter untuk verifikasi berapa kali method dipanggil
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
            ?: throw IllegalStateException(
                "FakeBlogsApi: blogByIdResponse not set"
            )
    }
}
