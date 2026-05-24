package org.example.project.features.blogs.data.mapper

import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.data.dto.BlogDto

fun BlogDto.toDomain(): BlogModel = BlogModel(
    userId = userId,
    id = id,
    title = title,
    body = body
)
