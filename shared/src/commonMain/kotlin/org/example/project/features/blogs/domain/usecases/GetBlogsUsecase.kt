package org.example.project.features.blogs.domain.usecases

import org.example.project.core.usecase.NoParamUseCase
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.domain.repository.BlogRepository

class GetBlogsUseCase(
    private val repository: BlogRepository,
): NoParamUseCase<Result<List<BlogModel>>> {

    override suspend operator fun invoke(): Result<List<BlogModel>> =
        repository.getBlogs()
}
