package org.example.project.features.blogs.fakes

import org.example.project.core.util.Failure
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.domain.repository.BlogRepository

class FakeBlogRepository : BlogRepository {

    // Default result yang akan dikembalikan
    var blogsResult: Result<List<BlogModel>> =
        Result.Success(emptyList())

    var blogByIdResult: Result<BlogModel> =
        Result.Error(
            Failure.UnknownFailure("blogByIdResult not set")
        )

    // Counter untuk verifikasi
    var getBlogsCallCount: Int = 0
    var getBlogByIdCallCount: Int = 0
    var lastRequestedId: Int? = null

    override suspend fun getBlogs(): Result<List<BlogModel>> {
        getBlogsCallCount++
        return blogsResult
    }

    override suspend fun getBlogById(id: Int): Result<BlogModel> {
        getBlogByIdCallCount++
        lastRequestedId = id
        return blogByIdResult
    }
}
