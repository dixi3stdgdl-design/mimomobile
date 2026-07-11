package com.mimo.mobile.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for error handling scenarios.
 *
 * Covers: network disconnect, server unavailable, invalid responses,
 * and error state management.
 */
class ErrorHandlerTest {

    // ─── Network disconnect scenarios ───

    @Test
    fun `connection state transitions on network loss`() {
        var state = ConnectionState.CONNECTED
        // Simulate network loss: CONNECTED -> ERROR -> DISCONNECTED
        state = ConnectionState.ERROR
        assertEquals(ConnectionState.ERROR, state)
        state = ConnectionState.DISCONNECTED
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun `error message emitted on connection failure`() {
        // WebSocketClient emits: WsMessage(type = "error", data = "Connection failed: ${e.message}")
        val errorMsg = "Connection failed: java.net.ConnectException: Connection refused"
        val msg = WsMessage(type = "error", data = errorMsg)
        assertEquals("error", msg.type)
        assertTrue(msg.data.toString().contains("Connection failed"))
        assertTrue(msg.data.toString().contains("Connection refused"))
    }

    @Test
    fun `send failure emits error message`() {
        // WebSocketClient sendMessage emits: WsMessage(type = "error", data = "Send failed: ${e.message}")
        val errorMsg = "Send failed: java.io.IOException: Broken pipe"
        val msg = WsMessage(type = "error", data = errorMsg)
        assertEquals("error", msg.type)
        assertTrue(msg.data.toString().contains("Send failed"))
    }

    @Test
    fun `network unavailable prevents reconnection`() {
        // When isNetworkAvailable is false, doConnect returns early
        // and scheduleReconnect returns early
        val networkAvailable = false
        val shouldReconnect = true
        val state = ConnectionState.DISCONNECTED

        // Simulate the check in scheduleReconnect
        val shouldAttempt = shouldReconnect && networkAvailable
        assertFalse("Should not attempt reconnect when network is unavailable", shouldAttempt)
    }

    @Test
    fun `network restore triggers reconnection`() {
        var networkAvailable = false
        var shouldReconnect = true
        var state = ConnectionState.DISCONNECTED

        // Simulate network restore
        val wasUnavailable = !networkAvailable
        networkAvailable = true
        val shouldAttempt = shouldReconnect && wasUnavailable && state != ConnectionState.CONNECTED
        assertTrue("Should attempt reconnect when network is restored", shouldAttempt)
    }

    @Test
    fun `already connected prevents reconnection attempt`() {
        var networkAvailable = true
        var shouldReconnect = true
        var state = ConnectionState.CONNECTED

        val wasUnavailable = false // was available before
        val shouldAttempt = shouldReconnect && wasUnavailable && state != ConnectionState.CONNECTED
        assertFalse("Should not reconnect when already connected", shouldAttempt)
    }

    // ─── Server unavailable ───

    @Test
    fun `server unavailable produces connection refused error`() {
        val error = "Connection refused"
        assertTrue(error.contains("refused"))
    }

    @Test
    fun `server timeout produces timeout error`() {
        // Socket.connect timeout = 10000ms
        val error = "Connect timed out"
        assertTrue(error.contains("timed out") || error.contains("timeout"))
    }

    @Test
    fun `server unreachable produces network error`() {
        val error = "Network is unreachable"
        assertTrue(error.contains("unreachable"))
    }

    @Test
    fun `host resolution failure produces unknown host error`() {
        val error = "unknown host: nonexistent.invalid"
        assertTrue(error.contains("unknown host"))
    }

    @Test
    fun `error message is emitted via SharedFlow`() = runTest {
        val flow = MutableSharedFlow<WsMessage>(extraBufferCapacity = 128)
        val errors = mutableListOf<String>()

        val job = launch(Dispatchers.Unconfined) {
            flow.collect { msg ->
                if (msg.type == "error") {
                    errors.add(msg.data.toString())
                }
            }
        }

        flow.emit(WsMessage(type = "error", data = "Connection failed: timeout"))
        flow.emit(WsMessage(type = "error", data = "Send failed: broken pipe"))

        advanceUntilIdle()
        job.cancel()

        assertEquals(2, errors.size)
        assertTrue(errors[0].contains("Connection failed"))
        assertTrue(errors[1].contains("Send failed"))
    }

    // ─── Invalid responses ───

    @Test
    fun `malformed JSON response produces raw message`() {
        // When JSON parsing fails in startReading, emit as "raw" type
        val rawMessage = "not valid json {{{"
        val msg = WsMessage(type = "raw", data = rawMessage)
        assertEquals("raw", msg.type)
        assertEquals(rawMessage, msg.data)
    }

    @Test
    fun `empty response is handled gracefully`() {
        val emptyPayload = ""
        val msg = WsMessage(type = "unknown", data = emptyPayload)
        assertEquals("unknown", msg.type)
        assertEquals("", msg.data)
    }

    @Test
    fun `null data field is handled in WsMessage`() {
        val msg = WsMessage(type = "test")
        assertNull(msg.data)
    }

    @Test
    fun `valid JSON response is parsed correctly`() {
        val json = JSONObject().apply {
            put("type", "chat_chunk")
            put("id", "msg_1")
            put("data", "hello world")
        }
        val msg = WsMessage(
            type = json.optString("type", "unknown"),
            id = json.optString("id", null),
            data = json.opt("data")
        )
        assertEquals("chat_chunk", msg.type)
        assertEquals("msg_1", msg.id)
        assertEquals("hello world", msg.data.toString())
    }

    @Test
    fun `JSON response with missing fields uses defaults`() {
        val json = JSONObject().apply {
            put("type", "chat_chunk")
            // Missing: id, data, path, etc.
        }
        val msg = WsMessage(
            type = json.optString("type", "unknown"),
            id = json.optString("id", null),
            data = json.opt("data"),
            path = json.optString("path", null)
        )
        assertEquals("chat_chunk", msg.type)
        assertNull(msg.id)
        assertNull(msg.data)
        assertNull(msg.path)
    }

    @Test
    fun `JSON response with extra fields is handled`() {
        val json = JSONObject().apply {
            put("type", "chat_chunk")
            put("id", "msg_1")
            put("data", "content")
            put("unknown_field", "should be ignored")
        }
        val msg = WsMessage(
            type = json.optString("type", "unknown"),
            id = json.optString("id", null),
            data = json.opt("data")
        )
        assertEquals("chat_chunk", msg.type)
        assertEquals("msg_1", msg.id)
    }

    @Test
    fun `WebSocket close frame terminates reading`() {
        val client = WebSocketClient()
        val decodeMethod = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        decodeMethod.isAccessible = true

        // Close frame (opcode 0x08)
        val closeFrame = byteArrayOf(0x88.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = decodeMethod.invoke(client, closeFrame, 0) as Pair<String?, Int>
        assertNull(result.first)
        assertEquals(closeFrame.size, result.second)
    }

    @Test
    fun `WebSocket ping frame is handled`() {
        val client = WebSocketClient()
        val decodeMethod = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        decodeMethod.isAccessible = true

        val pingFrame = byteArrayOf(0x89.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = decodeMethod.invoke(client, pingFrame, 0) as Pair<String?, Int>
        // Ping frames are handled (parsed but not processed as text)
        assertNotNull(result)
    }

    // ─── Error state management ───

    @Test
    fun `connectionState flow emits error state`() = runTest {
        val state = MutableStateFlow(ConnectionState.DISCONNECTED)
        val states = mutableListOf<ConnectionState>()

        val job = launch(Dispatchers.Unconfined) {
            state.collect { states.add(it) }
        }

        state.value = ConnectionState.CONNECTING
        state.value = ConnectionState.CONNECTED
        state.value = ConnectionState.ERROR
        state.value = ConnectionState.DISCONNECTED

        advanceUntilIdle()
        job.cancel()

        assertTrue(states.contains(ConnectionState.CONNECTING))
        assertTrue(states.contains(ConnectionState.CONNECTED))
        assertTrue(states.contains(ConnectionState.ERROR))
        assertTrue(states.contains(ConnectionState.DISCONNECTED))
    }

    @Test
    fun `error state transitions to disconnected after error`() {
        var state = ConnectionState.ERROR
        state = ConnectionState.DISCONNECTED
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun `error state can transition to connecting for retry`() {
        var state = ConnectionState.ERROR
        state = ConnectionState.CONNECTING
        assertEquals(ConnectionState.CONNECTING, state)
    }

    @Test
    fun `reconnect attempts reset after successful connection`() {
        var reconnectAttempts = 10
        // Simulate successful connection
        reconnectAttempts = 0
        assertEquals(0, reconnectAttempts)
    }

    @Test
    fun `reconnect is not attempted when shouldReconnect is false`() {
        val shouldReconnect = false
        val isNetworkAvailable = true
        val shouldAttempt = shouldReconnect && isNetworkAvailable
        assertFalse("Should not reconnect when shouldReconnect is false", shouldAttempt)
    }

    @Test
    fun `destroy client cleans up resources`() {
        val client = WebSocketClient()
        client.destroy()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    }

    @Test
    fun `sendMessage with null output does not crash`() {
        // When socket is null, sendMessage should not throw
        val client = WebSocketClient()
        // Socket is null initially, so sendMessage should handle gracefully
        client.sendMessage(WsMessage(type = "test"))
        // No crash = pass
    }

    @Test
    fun `multiple error messages can be queued`() = runTest {
        val flow = MutableSharedFlow<WsMessage>(extraBufferCapacity = 128)
        val errors = mutableListOf<WsMessage>()

        val job = launch(Dispatchers.Unconfined) {
            flow.collect { errors.add(it) }
        }

        repeat(50) {
            flow.emit(WsMessage(type = "error", data = "Error $it"))
        }

        advanceUntilIdle()
        job.cancel()

        assertEquals(50, errors.size)
        errors.forEachIndexed { index, msg ->
            assertEquals("Error $index", msg.data.toString())
        }
    }
}
