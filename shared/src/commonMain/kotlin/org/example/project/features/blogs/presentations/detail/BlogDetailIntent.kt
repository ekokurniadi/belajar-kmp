package org.example.project.features.blogs.presentations.detail

sealed class BlogDetailIntent {

    data class LoadBlog(val id: Int) : BlogDetailIntent()

    data object Retry : BlogDetailIntent()
}
