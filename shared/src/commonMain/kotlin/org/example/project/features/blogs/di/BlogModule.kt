package org.example.project.features.blogs.di


import org.example.project.features.blogs.data.datasources.api.BlogsApi
import org.example.project.features.blogs.data.datasources.api.BlogsApiImpl
import org.example.project.features.blogs.data.repository.BlogRepositoryImpl
import org.example.project.features.blogs.domain.repository.BlogRepository
import org.example.project.features.blogs.domain.usecases.GetBlogByIdUseCase
import org.example.project.features.blogs.domain.usecases.GetBlogsUseCase
import org.example.project.features.blogs.presentations.detail.BlogDetailViewModel
import org.example.project.features.blogs.presentations.list.BlogViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val blogModule = module {
    // Datasource
    singleOf(::BlogsApiImpl) {
        bind<BlogsApi>()
    }

    // Repository
    singleOf(::BlogRepositoryImpl) {
        bind<BlogRepository>()
    }

    // UseCases
    factoryOf(::GetBlogsUseCase)
    factoryOf(::GetBlogByIdUseCase)

    // ViewModels
    viewModelOf(::BlogViewModel)
    viewModelOf(::BlogDetailViewModel)
}
