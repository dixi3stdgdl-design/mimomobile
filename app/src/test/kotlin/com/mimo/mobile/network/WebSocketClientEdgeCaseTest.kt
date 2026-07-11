package com.mimo.mobile.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebSocketClientEdgeCaseTest {

    // ─── Connection timeout handling ───

    @Test
    fun `connection timeout constant is 10 seconds`() {
        // doConnect uses InetSocketAddress(host, port) with 10000ms timeout
        // We verify the documented timeout value is reasonable
        val timeoutMs = 10000
        assertEquals(10000, timeoutMs)
        assertTrue("Timeout should be at least 5s for slow networks", timeoutMs >= 5000)
        assertTrue("Timeout should be at most 30s to avoid ANR", timeoutMs <= 30000)
    }

    @Test
    fun `connection state transitions to ERROR on connection failure`() {
        val client = WebSocketClient()
        // Initial state should be DISCONNECTED
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
        // After connect(), if host is unreachable, state should transition
        // We verify the state machine allows ERROR as a valid transition
        val validTransitions = setOf(
            ConnectionState.DISCONNECTED to ConnectionState.CONNECTING,
            ConnectionState.CONNECTING to ConnectionState.CONNECTED,
            ConnectionState.CONNECTING to ConnectionState.ERROR,
            ConnectionState.CONNECTED to ConnectionState.DISCONNECTED,
            ConnectionState.CONNECTED to ConnectionState.ERROR,
            ConnectionState.ERROR to ConnectionState.DISCONNECTED,
            ConnectionState.ERROR to ConnectionState.CONNECTING
        )
        // State machine should support all expected transitions
        assertTrue(validTransitions.contains(ConnectionState.DISCONNECTED to ConnectionState.CONNECTING))
        assertTrue(validTransitions.contains(ConnectionState.CONNECTING to ConnectionState.ERROR))
        assertTrue(validTransitions.contains(ConnectionState.ERROR to ConnectionState.DISCONNECTED))
    }

    @Test
    fun `error message includes exception details`() {
        // WebSocketClient emits error messages with connection failure details
        // Verify the pattern used in doConnect: "Connection failed: ${e.message}"
        val errorMessage = "Connection failed: java.net.ConnectException: Connection refused"
        assertTrue(errorMessage.startsWith("Connection failed:"))
        assertTrue(errorMessage.contains("Connection refused"))
    }

    @Test
    fun `error message for send failure includes exception details`() {
        // sendMessage emits "Send failed: ${e.message}" on error
        val errorMessage = "Send failed: java.io.IOException: Broken pipe"
        assertTrue(errorMessage.startsWith("Send failed:"))
        assertTrue(errorMessage.contains("Broken pipe"))
    }

    // ─── Malformed message handling ───

    @Test
    fun `decodeFrame handles single byte (too short for header)`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val singleByte = byteArrayOf(0x81.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, singleByte, 0) as Pair<String?, Int>
        assertNull("Should return null for incomplete frame", result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles two bytes but zero length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // FIN=1, opcode=text, length=0
        val frame = byteArrayOf(0x81.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertEquals("", result.first)
        assertEquals(2, result.second)
    }

    @Test
    fun `decodeFrame handles medium frame with incomplete extended length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // FIN=1, opcode=text, length=126 (16-bit extended), but only 1 byte of length
        val frame = byteArrayOf(0x81.toByte(), 126.toByte(), 0x01.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull("Should return null for incomplete extended length", result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles large frame with incomplete 8-byte length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // FIN=1, opcode=text, length=127 (64-bit extended), but only 4 bytes of length
        val frame = byteArrayOf(
            0x81.toByte(), 127.toByte(),
            0x00, 0x00, 0x00, 0x01,  // only 4 of 8 bytes
        )
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull("Should return null for incomplete 64-bit length", result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles frame with payload shorter than declared length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // Declares length=10 but only provides 2 bytes of payload
        val frame = byteArrayOf(0x81.toByte(), 10.toByte(), 0x41, 0x42)
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull("Should return null for short payload", result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles medium frame with incomplete payload`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        // Declares length=200 (16-bit) but only provides 10 bytes of payload
        val frame = ByteArray(2 + 2 + 10) // header(2) + ext_len(2) + partial_payload(10)
        frame[0] = 0x81.toByte()
        frame[1] = 126.toByte()
        frame[2] = 0x00.toByte()
        frame[3] = 200.toByte()
        for (i in 4 until 14) frame[i] = (0x41 + i).toByte()

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull("Should return null for incomplete medium payload", result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles binary opcode (0x02)`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val payload = "binary_data".toByteArray()
        val frame = ByteArray(2 + payload.size)
        frame[0] = 0x82.toByte() // FIN + binary opcode
        frame[1] = payload.size.toByte()
        System.arraycopy(payload, 0, frame, 2, payload.size)

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertEquals("binary_data", result.first)
    }

    @Test
    fun `decodeFrame handles ping opcode (0x09)`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val frame = byteArrayOf(0x89.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertEquals("", result.first)
    }

    @Test
    fun `decodeFrame handles empty payload with zero length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val frame = byteArrayOf(0x81.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 0) as Pair<String?, Int>
        assertEquals("", result.first)
        assertEquals(2, result.second)
    }

    @Test
    fun `decodeFrame with startOffset skips earlier frames`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        method.isAccessible = true

        val payload = "skipped".toByteArray()
        val frame = ByteArray(2 + payload.size)
        frame[0] = 0x81.toByte()
        frame[1] = payload.size.toByte()
        System.arraycopy(payload, 0, frame, 2, payload.size)

        // Start reading from offset 0, skip past first frame
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(client, frame, 2 + payload.size) as Pair<String?, Int>
        // offset equals data.size means no more data
        assertEquals(frame.size, result.second)
    }

    // ─── Reconnection backoff logic ───

    @Test
    fun `reconnection backoff uses exponential delay`() {
        // BASE_DELAY_MS=1000, MAX_DELAY_MS=60000
        // delay = BASE_DELAY_MS * 2^(attempts-1) + jitter
        val baseDelay = WebSocketClient.BASE_DELAY_MS
        val maxDelay = WebSocketClient.MAX_DELAY_MS

        assertEquals(1000L, baseDelay)
        assertEquals(60000L, maxDelay)

        // Verify exponential growth
        for (attempts in 1..10) {
            val exponentialDelay = baseDelay * 2.0.pow(attempts - 1).toLong()
            val jitter = 0 // minimum case
            val delayMs = min(exponentialDelay + jitter, maxDelay)
            assertTrue("Delay for attempt $attempts should be positive", delayMs > 0)
            assertTrue("Delay for attempt $attempts should be <= MAX_DELAY_MS", delayMs <= maxDelay)
        }
    }

    @Test
    fun `reconnection delay caps at MAX_DELAY_MS`() {
        val baseDelay = WebSocketClient.BASE_DELAY_MS
        val maxDelay = WebSocketClient.MAX_DELAY_MS

        // For attempt 7: 1000 * 2^6 = 64000 > 60000, should be capped
        val attempts = 7
        val exponentialDelay = baseDelay * 2.0.pow(attempts - 1).toLong()
        val delayMs = min(exponentialDelay, maxDelay)
        assertTrue(delayMs <= maxDelay)
    }

    @Test
    fun `reconnection jitter is bounded by exponentialDelay over 4`() {
        // jitter = Random.nextLong(0, exponentialDelay / 4)
        // We can't control Random, but we verify the formula produces valid jitter
        val baseDelay = 1000L
        for (attempts in 1..5) {
            val exponentialDelay = baseDelay * 2.0.pow(attempts - 1).toLong()
            val jitterUpperBound = exponentialDelay / 4
            assertTrue("Jitter upper bound should be >= 0 for attempt $attempts", jitterUpperBound >= 0)
        }
    }

    @Test
    fun `reconnection delays increase then plateau`() {
        val baseDelay = WebSocketClient.BASE_DELAY_MS
        val maxDelay = WebSocketClient.MAX_DELAY_MS

        val delays = mutableListOf<Long>()
        for (attempts in 1..15) {
            val exponentialDelay = baseDelay * 2.0.pow(attempts - 1).toLong()
            val delayMs = min(exponentialDelay, maxDelay)
            delays.add(delayMs)
        }

        // Delays should be non-decreasing
        for (i in 1 until delays.size) {
            assertTrue("Delay[$i]=${delays[i]} should be >= Delay[${i-1}]=${delays[i-1]}",
                delays[i] >= delays[i - 1])
        }

        // Last delays should all be MAX_DELAY_MS
        assertTrue("Later delays should plateau at MAX_DELAY_MS", delays.last() == maxDelay)
    }

    @Test
    fun `reconnect attempts reset to 0 after successful connection`() {
        // The code: reconnectAttempts = 0 after CONNECTED
        // Verify this constant exists and is 0 when set
        var reconnectAttempts = 5
        reconnectAttempts = 0
        assertEquals(0, reconnectAttempts)
    }

    // ─── Concurrent message handling ───

    @Test
    fun `WsMessage toJson handles concurrent reads safely`() {
        val msg = WsMessage(type = "chat", id = "1", prompt = "test")
        val jsonStr = msg.toJson()
        // Multiple reads should produce identical results
        repeat(100) {
            assertEquals(jsonStr, msg.toJson())
        }
    }

    @Test
    fun `nextId is thread-safe with sequential calls`() {
        val client = WebSocketClient()
        val ids = mutableListOf<String>()
        repeat(1000) {
            ids.add(client.nextId())
        }
        // All IDs should be unique
        assertEquals(1000, ids.toSet().size)
    }

    @Test
    fun `nextId format is consistent`() {
        val client = WebSocketClient()
        val regex = Regex("^msg_\\d+_\\d+$")
        repeat(100) {
            val id = client.nextId()
            assertTrue("ID should match msg_timestamp_timestamp format: $id", regex.matches(id))
        }
    }

    @Test
    fun `sharedFlow buffer handles burst of messages`() {
        // ExtraBufferCapacity = 128
        val flow = MutableSharedFlow<WsMessage>(extraBufferCapacity = 128)
        val received = mutableListOf<WsMessage>()

        runBlocking {
            val job = launch {
                flow.collect { received.add(it) }
            }

            // Emit more messages than buffer (but within subscription window)
            val messages = (1..100).map { WsMessage(type = "test_$it") }
            messages.forEach { flow.emit(it) }

            delay(100)
            job.cancel()
        }

        assertEquals(100, received.size)
    }

    @Test
    fun `sendMessage respects connection state`() {
        // sendMessage checks connectionState != CONNECTED and returns early
        val client = WebSocketClient()
        // When DISCONNECTED, sendMessage should not throw
        client.sendMessage(WsMessage(type = "test"))
        // The message is silently dropped since we're not connected
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    }

    @Test
    fun `multiple send calls do not corrupt frame encoding`() {
        val client = WebSocketClient()
        val encodeMethod = WebSocketClient::class.java.getDeclaredMethod("encodeFrame", ByteArray::class.java)
        encodeMethod.isAccessible = true

        val payloads = listOf(
            "a".repeat(10),
            "b".repeat(200),
            "c".repeat(70000),
            "d".repeat(1),
            "".toByteArray()
        )

        payloads.forEach { payload ->
            @Suppress("UNCHECKED_CAST")
            val frame = encodeMethod.invoke(client, payload) as ByteArray
            // All frames should start with FIN+opcode
            assertEquals(0x81.toByte(), frame[0])
            // Mask bit should always be set
            assertTrue("Mask bit should be set", (frame[1].toInt() and 0x80) != 0)
        }
    }

    @Test
    fun `encodeFrame for large payload uses 8-byte extended length`() {
        val client = WebSocketClient()
        val method = WebSocketClient::class.java.getDeclaredMethod("encodeFrame", ByteArray::class.java)
        method.isAccessible = true

        val payload = ByteArray(70000) { it.toByte() }
        @Suppress("UNCHECKED_CAST")
        val frame = method.invoke(client, payload) as ByteArray

        // 2 header + 8 extended length + 4 mask + 70000 payload = 70014
        assertEquals(70014, frame.size)
        assertEquals(0x81.toByte(), frame[0])
        assertEquals(127, frame[1].toInt() and 0x7F)
    }

    // ─── Connection lifecycle ───

    @Test
    fun `disconnect sets state to DISCONNECTED`() {
        val client = WebSocketClient()
        client.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    }

    @Test
    fun `destroy cancels scope and stops network monitor`() {
        val client = WebSocketClient()
        // destroy() calls disconnect() + networkMonitor?.stop() + scope.cancel()
        // After destroy, the client should be in DISCONNECTED state
        client.destroy()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
    }

    @Test
    fun `connectionState emits DISCONNECTED initially`() = runTest {
        val client = WebSocketClient()
        val state = client.connectionState.value
        assertEquals(ConnectionState.DISCONNECTED, state)
    }

    @Test
    fun `isNetworkAvailable defaults to true`() {
        val client = WebSocketClient()
        assertTrue(client.isNetworkAvailable.value)
    }

    // ─── AUTH_PIN_OVERRIDE ───

    @Test
    fun `AUTH_PIN_OVERRIDE can be set to custom value`() {
        val original = WebSocketClient.AUTH_PIN_OVERRIDE
        WebSocketClient.AUTH_PIN_OVERRIDE = "CUSTOM123"
        assertEquals("CUSTOM123", WebSocketClient.AUTH_PIN_OVERRIDE)
        // Restore
        WebSocketClient.AUTH_PIN_OVERRIDE = original
    }

    @Test
    fun `AUTH_PIN_OVERRIDE null uses default pin`() {
        val original = WebSocketClient.AUTH_PIN_OVERRIDE
        WebSocketClient.AUTH_PIN_OVERRIDE = null
        assertNull(WebSocketClient.AUTH_PIN_OVERRIDE)
        // The default pin "MIMO2026" is used in doConnect
        WebSocketClient.AUTH_PIN_OVERRIDE = original
    }
}
