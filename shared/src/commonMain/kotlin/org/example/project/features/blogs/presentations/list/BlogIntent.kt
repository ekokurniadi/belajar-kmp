package org.example.project.features.blogs.presentations.list

sealed class BlogIntent {

    data object LoadBlogs : BlogIntent()

    data object Refresh : BlogIntent()

    data class BlogClicked(val id: Int) : BlogIntent()
}
