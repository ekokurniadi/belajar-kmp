package org.example.project.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Tests untuk Failure sealed class.
 *
 * Failure adalah typed error untuk Result.Error:
 * - Failure.NetworkFailure → error koneksi
 * - Failure.UnknownFailure → error tak terduga
 *
 * Pattern ini mirip abstract class Failure + subclasses
 * di Flutter (dengan dartz Either<Failure, T>).
 */
class FailureTest {

    // ============================================================
    //  NetworkFailure
    // ============================================================

    @Test
    fun `NetworkFailure stores message`() {

        val failure = Failure.NetworkFailure("Connection lost")

        assertEquals("Connection lost", failure.message)
    }

    @Test
    fun `NetworkFailure exposes message via parent Failure property`() {

        // Type sebagai parent Failure
        val failure: Failure = Failure.NetworkFailure("Offline")

        // .message accessible via parent open val
        assertEquals("Offline", failure.message)
    }

    // ============================================================
    //  UnknownFailure
    // ============================================================

    @Test
    fun `UnknownFailure stores message`() {

        val failure = Failure.UnknownFailure("Something broke")

        assertEquals("Something broke", failure.message)
    }

    @Test
    fun `UnknownFailure exposes message via parent Failure property`() {

        val failure: Failure = Failure.UnknownFailure("Boom")

        assertEquals("Boom", failure.message)
    }

    // ============================================================
    //  Pattern Matching (when block)
    // ============================================================

    @Test
    fun `when block distinguishes NetworkFailure`() {

        val failure: Failure = Failure.NetworkFailure("offline")

        val type = when (failure) {
            is Failure.NetworkFailure -> "network"
            is Failure.UnknownFailure -> "unknown"
        }

        assertEquals("network", type)
    }

    @Test
    fun `when block distinguishes UnknownFailure`() {

        val failure: Failure = Failure.UnknownFailure("bug")

        val type = when (failure) {
            is Failure.NetworkFailure -> "network"
            is Failure.UnknownFailure -> "unknown"
        }

        assertEquals("unknown", type)
    }

    // ============================================================
    //  Data Class Equality
    // ============================================================

    @Test
    fun `Two NetworkFailure with same message are equal`() {

        val a = Failure.NetworkFailure("same")
        val b = Failure.NetworkFailure("same")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `Two NetworkFailure with different messages are NOT equal`() {

        val a = Failure.NetworkFailure("a")
        val b = Failure.NetworkFailure("b")

        assertNotEquals(a, b)
    }

    @Test
    fun `NetworkFailure and UnknownFailure with same message are NOT equal`() {

        val network: Failure = Failure.NetworkFailure("oops")
        val unknown: Failure = Failure.UnknownFailure("oops")

        // Meski message-nya sama, tipe-nya beda
        assertNotEquals(network, unknown)
    }

    // ============================================================
    //  Type Discrimination
    // ============================================================

    @Test
    fun `assertIs narrows to specific subtype`() {

        val failure: Failure = Failure.NetworkFailure("offline")

        val network = assertIs<Failure.NetworkFailure>(failure)
        assertEquals("offline", network.message)
    }
}
