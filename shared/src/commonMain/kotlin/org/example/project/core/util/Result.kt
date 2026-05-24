package org.example.project.core.util

sealed class Result<out T> {
    data class Success<T>(val data: T): Result<T>()
    data class Error(val failure: Failure): Result<Nothing>()
}

