package org.example.project.features.blogs.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BlogDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)
