package org.example.project.features.blogs.data.repository

import org.example.project.core.util.Failure
import org.example.project.core.util.Result
import org.example.project.features.blogs.data.datasources.api.BlogsApi
import org.example.project.features.blogs.data.dto.BlogDto
import org.example.project.features.blogs.data.mapper.toDomain
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.domain.repository.BlogRepository

class BlogRepositoryImpl(
    private val blogsApi: BlogsApi,
) : BlogRepository {

    override suspend fun getBlogs(): Result<List<BlogModel>> {
        return try {
            val response: List<BlogDto> = blogsApi.getBlogs()
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(
                failure = Failure.NetworkFailure(
                    message = e.message ?: "Unknown Error"
                )
            )
        }
    }

    override suspend fun getBlogById(id: Int): Result<BlogModel> {
        return try {
            val response: BlogDto = blogsApi.getBlogById(id)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(
                failure = Failure.NetworkFailure(
                    message = e.message ?: "Unknown Error"
                )
            )
        }
    }
}
