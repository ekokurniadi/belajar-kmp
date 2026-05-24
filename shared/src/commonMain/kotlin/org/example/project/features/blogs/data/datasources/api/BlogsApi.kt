package org.example.project.features.blogs.data.datasources.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.features.blogs.data.dto.BlogDto

interface BlogsApi {

    suspend fun getBlogs(): List<BlogDto>

    suspend fun getBlogById(id: Int): BlogDto
}

class BlogsApiImpl(
    private val httpClient: HttpClient,
) : BlogsApi {

    override suspend fun getBlogs(): List<BlogDto> =
        httpClient.get("posts").body()

    override suspend fun getBlogById(id: Int): BlogDto =
        httpClient.get("posts/$id").body()
}
