package org.example.project.features.blogs.domain.usecases

import org.example.project.core.usecase.UseCase
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.domain.repository.BlogRepository

class GetBlogByIdUseCase(
    private val repository: BlogRepository,
) : UseCase<Int, Result<BlogModel>> {

    override suspend operator fun invoke(param: Int): Result<BlogModel> =
        repository.getBlogById(param)
}
