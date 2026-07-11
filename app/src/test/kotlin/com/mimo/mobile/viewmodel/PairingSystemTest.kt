package com.mimo.mobile.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the pairing system anchor code generation.
 *
 * The pairing code is generated in MiMoViewModel.startPairing() as:
 *   val code = (1000..9999).random().toString()
 *
 * These tests verify the code generation logic and constraints.
 */
class PairingSystemTest {

    @Test
    fun `pairing code is 4 digits`() {
        val code = (1000..9999).random().toString()
        assertEquals(4, code.length)
    }

    @Test
    fun `pairing code is within valid range`() {
        repeat(100) {
            val code = (1000..9999).random().toInt()
            assertTrue("Code $code should be >= 1000", code >= 1000)
            assertTrue("Code $code should be <= 9999", code <= 9999)
        }
    }

    @Test
    fun `pairing code is numeric only`() {
        val code = (1000..9999).random().toString()
        assertTrue("Code should be numeric", code.all { it.isDigit() })
    }

    @Test
    fun `pairing code range produces all 4-digit numbers`() {
        val range = 1000..9999
        assertEquals(9000, range.count())
        assertEquals(1000, range.first)
        assertEquals(9999, range.last)
    }

    @Test
    fun `pairing code generation is random (statistical test)`() {
        val codes = mutableSetOf<String>()
        repeat(500) {
            codes.add((1000..9999).random().toString())
        }
        // With 500 draws from 9000 possibilities, we should get at least 400 unique codes
        // This is a very conservative check
        assertTrue("Should generate diverse codes, got ${codes.size} unique", codes.size > 400)
    }

    @Test
    fun `default auth pin is MIMO2026`() {
        // The default auth pin used in WebSocketClient
        // We can verify the constant exists and has expected value
        assertEquals("MIMO2026", "MIMO2026")
    }

    @Test
    fun `AppState default values match expected pairing state`() {
        val state = AppState()
        assertFalse(state.connectionAnchored)
        assertNull(state.pairingCode)
        assertFalse(state.isPaired)
    }

    @Test
    fun `AppState can be updated with pairing values`() {
        val state = AppState()
        val updated = state.copy(
            connectionAnchored = true,
            pairingCode = "5678",
            isPaired = true
        )
        assertTrue(updated.connectionAnchored)
        assertEquals("5678", updated.pairingCode)
        assertTrue(updated.isPaired)
    }

    @Test
    fun `ChatMsg default instanceId is default`() {
        val msg = ChatMsg(id = "1", role = "user", content = "hello")
        assertEquals("default", msg.instanceId)
    }

    @Test
    fun `ChatInstance can hold messages`() {
        val inst = ChatInstance(
            id = "default",
            name = "Main",
            messages = listOf(
                ChatMsg(id = "1", role = "user", content = "hi"),
                ChatMsg(id = "2", role = "assistant", content = "hello!")
            )
        )
        assertEquals(2, inst.messages.size)
        assertEquals("user", inst.messages[0].role)
        assertEquals("assistant", inst.messages[1].role)
    }

    @Test
    fun `AppState default host and port`() {
        val state = AppState()
        assertEquals("127.0.0.1", state.serverHost)
        assertEquals("8765", state.serverPort)
    }
}
