package org.example.project.features.blogs.presentations.list

sealed class BlogEffect {

    data class ShowSnackbar(val message: String) : BlogEffect()

    data class NavigateToDetail(val blogId: Int) : BlogEffect()
}
