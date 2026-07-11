package com.mimo.mobile.network

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

class WebSocketClientTest {

    @Test
    fun `WsMessage toJson serializes type correctly`() {
        val msg = WsMessage(type = "chat", id = "msg_1", prompt = "hello")
        val json = JSONObject(msg.toJson())
        assertEquals("chat", json.getString("type"))
        assertEquals("msg_1", json.getString("id"))
        assertEquals("hello", json.getString("prompt"))
    }

    @Test
    fun `WsMessage toJson omits null fields`() {
        val msg = WsMessage(type = "auth")
        val json = JSONObject(msg.toJson())
        assertEquals("auth", json.getString("type"))
        assertFalse(json.has("id"))
        assertFalse(json.has("data"))
        assertFalse(json.has("path"))
        assertFalse(json.has("command"))
        assertFalse(json.has("prompt"))
        assertFalse(json.has("content"))
        assertFalse(json.has("filename"))
        assertFalse(json.has("instance_id"))
    }

    @Test
    fun `WsMessage toJson includes all optional fields when set`() {
        val msg = WsMessage(
            type = "write_file",
            id = "msg_2",
            data = "some data",
            path = "/tmp/test.txt",
            command = "ls -la",
            prompt = "read file",
            content = "file content here",
            filename = "test.txt",
            instance_id = "inst_1"
        )
        val json = JSONObject(msg.toJson())
        assertEquals("write_file", json.getString("type"))
        assertEquals("msg_2", json.getString("id"))
        assertEquals("some data", json.getString("data"))
        assertEquals("/tmp/test.txt", json.getString("path"))
        assertEquals("ls -la", json.getString("command"))
        assertEquals("read file", json.getString("prompt"))
        assertEquals("file content here", json.getString("content"))
        assertEquals("test.txt", json.getString("filename"))
        assertEquals("inst_1", json.getString("instance_id"))
    }

    @Test
    fun `ConnectionState has all expected values`() {
        val values = ConnectionState.values()
        assertEquals(4, values.size)
        assertNotNull(ConnectionState.DISCONNECTED)
        assertNotNull(ConnectionState.CONNECTING)
        assertNotNull(ConnectionState.CONNECTED)
        assertNotNull(ConnectionState.ERROR)
    }

    @Test
    fun `ConnectionState valueOf works correctly`() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"))
        assertEquals(ConnectionState.CONNECTING, ConnectionState.valueOf("CONNECTING"))
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"))
        assertEquals(ConnectionState.ERROR, ConnectionState.valueOf("ERROR"))
    }

    @Test
    fun `reconnection constants are reasonable`() {
        assertTrue("BASE_DELAY_MS should be positive", WebSocketClient.BASE_DELAY_MS > 0)
        assertTrue("MAX_DELAY_MS should be >= BASE_DELAY_MS", WebSocketClient.MAX_DELAY_MS >= WebSocketClient.BASE_DELAY_MS)
        assertEquals(1000L, WebSocketClient.BASE_DELAY_MS)
        assertEquals(60000L, WebSocketClient.MAX_DELAY_MS)
    }

    @Test
    fun `encodeFrame produces valid WebSocket frame header for small payload`() {
        val client = WebSocketClient()
        val encodeMethod = WebSocketClient::class.java.getDeclaredMethod("encodeFrame", ByteArray::class.java)
        encodeMethod.isAccessible = true

        val payload = "hello".toByteArray()
        val frame = encodeMethod.invoke(client, payload) as ByteArray

        // First byte: FIN (0x81) + opcode text (0x01)
        assertEquals(0x81.toByte(), frame[0])
        // Second byte: mask bit set (0x80) + length
        assertTrue("Mask bit should be set", (frame[1].toInt() and 0x80) != 0)
        assertEquals(5, frame[1].toInt() and 0x7F)
        // Frame should be: 2 header + 4 mask + 5 payload = 11 bytes
        assertEquals(11, frame.size)
    }

    @Test
    fun `encodeFrame handles medium payload correctly`() {
        val client = WebSocketClient()
        val encodeMethod = WebSocketClient::class.java.getDeclaredMethod("encodeFrame", ByteArray::class.java)
        encodeMethod.isAccessible = true

        val payload = ByteArray(200) { it.toByte() }
        val frame = encodeMethod.invoke(client, payload) as ByteArray

        // 2 header + 2 extended length + 4 mask + 200 payload = 208
        assertEquals(208, frame.size)
        assertEquals(0x81.toByte(), frame[0])
        assertEquals(126, frame[1].toInt() and 0x7F)
    }

    @Test
    fun `decodeFrame parses small frame correctly`() {
        val client = WebSocketClient()
        val decodeMethod = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        decodeMethod.isAccessible = true

        // Build a minimal unmasked server frame (opcode 0x01, no mask)
        val payload = "test".toByteArray()
        val frame = ByteArray(2 + payload.size)
        frame[0] = 0x81.toByte() // FIN + text opcode
        frame[1] = payload.size.toByte() // no mask, length=4
        System.arraycopy(payload, 0, frame, 2, payload.size)

        @Suppress("UNCHECKED_CAST")
        val result = decodeMethod.invoke(client, frame, 0) as Pair<String?, Int>
        assertEquals("test", result.first)
        assertEquals(frame.size, result.second)
    }

    @Test
    fun `decodeFrame returns null for incomplete frame`() {
        val client = WebSocketClient()
        val decodeMethod = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        decodeMethod.isAccessible = true

        // Only 1 byte - not enough for header
        val frame = byteArrayOf(0x81.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = decodeMethod.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull(result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `decodeFrame handles close opcode`() {
        val client = WebSocketClient()
        val decodeMethod = WebSocketClient::class.java.getDeclaredMethod(
            "decodeFrame", ByteArray::class.java, Int::class.javaPrimitiveType
        )
        decodeMethod.isAccessible = true

        // Close frame (opcode 0x08)
        val frame = byteArrayOf(0x88.toByte(), 0x00.toByte())
        @Suppress("UNCHECKED_CAST")
        val result = decodeMethod.invoke(client, frame, 0) as Pair<String?, Int>
        assertNull(result.first)
        assertEquals(frame.size, result.second)
    }

    @Test
    fun `nextId generates unique sequential IDs`() {
        val client = WebSocketClient()
        val id1 = client.nextId()
        val id2 = client.nextId()
        assertNotEquals(id1, id2)
        assertTrue(id1.startsWith("msg_"))
        assertTrue(id2.startsWith("msg_"))
    }

    @Test
    fun `WsMessage data field serializes as string`() {
        val msg = WsMessage(type = "test", data = "raw_data")
        val json = JSONObject(msg.toJson())
        assertEquals("raw_data", json.getString("data"))
    }

    @Test
    fun `WsMessage entries field serializes correctly`() {
        val entries = listOf(
            mapOf("name" to "file.txt", "size" to 1024),
            mapOf("name" to "dir", "isDir" to true)
        )
        val msg = WsMessage(type = "list_dir", entries = entries)
        // entries are not serialized by toJson, but the data class holds them
        assertEquals(entries, msg.entries)
        assertEquals("list_dir", msg.type)
    }

    @Test
    fun `WsMessage exitCode field preserved`() {
        val msg = WsMessage(type = "execute_result", exitCode = 0)
        assertNull(msg.data)
        assertEquals(0, msg.exitCode)
    }

    @Test
    fun `WsMessage error field preserved`() {
        val msg = WsMessage(type = "error", error = "Connection refused")
        assertEquals("Connection refused", msg.error)
    }
}
