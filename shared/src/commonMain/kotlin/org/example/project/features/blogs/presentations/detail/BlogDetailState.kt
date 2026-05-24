package org.example.project.features.blogs.presentations.detail

import org.example.project.features.blogs.domain.model.BlogModel

sealed interface BlogDetailState {

    data object Initial : BlogDetailState

    data object Loading : BlogDetailState

    data class Success(
        val blog: BlogModel,
    ) : BlogDetailState

    data class Failure(
        val message: String,
    ) : BlogDetailState
}
