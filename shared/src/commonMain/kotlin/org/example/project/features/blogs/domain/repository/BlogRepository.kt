package org.example.project.features.blogs.domain.repository

import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel

interface BlogRepository {

    suspend fun getBlogs(): Result<List<BlogModel>>

    suspend fun getBlogById(id: Int): Result<BlogModel>
}
