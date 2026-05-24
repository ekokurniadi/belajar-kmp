package org.example.project.core.usecase

interface UseCase<in P, out R> {
    suspend fun invoke(param: P): R
}

interface NoParamUseCase<out R> {
    suspend fun invoke(): R
}