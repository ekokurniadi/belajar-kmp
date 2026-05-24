package org.example.project.features.blogs.presentations.detail

sealed class BlogDetailEffect {

    data class ShowSnackbar(val message: String) : BlogDetailEffect()
}
