package org.example.project.core.util

sealed class Failure(open val message: String) {
    data class NetworkFailure(override val message: String): Failure(message)
    data class UnknownFailure(override val message: String): Failure(message)
}