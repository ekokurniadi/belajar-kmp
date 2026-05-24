package org.example.project.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Tests untuk Result sealed class.
 *
 * Result adalah wrapper yang membungkus output operasi:
 * - Result.Success(data) → operasi sukses
 * - Result.Error(failure) → operasi gagal
 *
 * Mirip Either<Failure, T> di dartz Flutter.
 */
class ResultTest {

    // ============================================================
    //  Success
    // ============================================================

    @Test
    fun `Success holds the data passed to it`() {

        // Given
        val expectedData = "Hello World"

        // When
        val result: Result<String> = Result.Success(expectedData)

        // Then
        val success = assertIs<Result.Success<String>>(result)
        assertEquals(expectedData, success.data)
    }

    @Test
    fun `Success works with any type (generic)`() {

        // String
        val stringResult: Result<String> = Result.Success("text")
        assertEquals("text", (stringResult as Result.Success).data)

        // Int
        val intResult: Result<Int> = Result.Success(42)
        assertEquals(42, (intResult as Result.Success).data)

        // List
        val listResult: Result<List<Int>> =
            Result.Success(listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), (listResult as Result.Success).data)
    }

    // ============================================================
    //  Error
    // ============================================================

    @Test
    fun `Error holds the failure passed to it`() {

        // Given
        val failure = Failure.NetworkFailure("No internet")

        // When
        val result: Result<String> = Result.Error(failure)

        // Then
        val error = assertIs<Result.Error>(result)
        assertEquals(failure, error.failure)
        assertEquals("No internet", error.failure.message)
    }

    // ============================================================
    //  Pattern Matching (when block)
    // ============================================================

    @Test
    fun `when block exhaustive with smart cast on Success`() {

        // Given
        val result: Result<Int> = Result.Success(42)

        // When - pattern matching dengan smart cast
        val output = when (result) {
            is Result.Success -> "Got ${result.data}"  // ← smart cast
            is Result.Error -> "Error: ${result.failure.message}"
        }

        // Then
        assertEquals("Got 42", output)
    }

    @Test
    fun `when block exhaustive with smart cast on Error`() {

        // Given
        val result: Result<Int> = Result.Error(
            Failure.NetworkFailure("offline")
        )

        // When
        val output = when (result) {
            is Result.Success -> "Got ${result.data}"
            is Result.Error -> "Error: ${result.failure.message}"
        }

        // Then
        assertEquals("Error: offline", output)
    }

    // ============================================================
    //  Data Class Equality
    // ============================================================

    @Test
    fun `Two Success with same data are equal (data class)`() {

        val a = Result.Success("hello")
        val b = Result.Success("hello")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `Two Success with different data are NOT equal`() {

        val a = Result.Success("hello")
        val b = Result.Success("world")

        assertNotEquals<Result<String>>(a, b)
    }

    @Test
    fun `Success and Error are NOT equal`() {

        val success: Result<String> = Result.Success("data")
        val error: Result<String> = Result.Error(
            Failure.UnknownFailure("err")
        )

        assertNotEquals(success, error)
    }
}
