package org.example.project.features.blogs.presentations.list

import org.example.project.features.blogs.domain.model.BlogModel

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
