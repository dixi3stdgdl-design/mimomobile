package com.mimo.mobile.network

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

data class WsMessage(
    val type: String,
    val id: String? = null,
    val data: Any? = null,
    val path: String? = null,
    val entries: List<Map<String, Any>>? = null,
    val exitCode: Int? = null,
    val error: String? = null,
    val command: String? = null,
    val prompt: String? = null,
    val content: String? = null,
    val filename: String? = null,
    val instance_id: String? = null
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", type)
        id?.let { obj.put("id", it) }
        data?.let { obj.put("data", it.toString()) }
        path?.let { obj.put("path", it) }
        command?.let { obj.put("command", it) }
        prompt?.let { obj.put("prompt", it) }
        content?.let { obj.put("content", it) }
        filename?.let { obj.put("filename", it) }
        instance_id?.let { obj.put("instance_id", it) }
        return obj.toString()
    }
}

class WebSocketClient {
    companion object {
        var AUTH_PIN_OVERRIDE: String? = null
        private const val DEFAULT_AUTH_PIN = "MIMO2026"
        const val BASE_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 60000L
    }

    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<WsMessage>(extraBufferCapacity = 128)
    val messages: SharedFlow<WsMessage> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private var requestId = 0
    private var shouldReconnect = false
    private var currentHost = ""
    private var currentPort = 0
    private var reconnectAttempts = 0
    private var networkMonitor: NetworkMonitor? = null

    fun initNetworkMonitor(context: Context) {
        networkMonitor = NetworkMonitor(context)
        networkMonitor!!.start()

        scope.launch {
            networkMonitor!!.isAvailable.collect { available ->
                val wasUnavailable = !_isNetworkAvailable.value
                _isNetworkAvailable.value = available
                if (available && wasUnavailable && shouldReconnect && _connectionState.value != ConnectionState.CONNECTED) {
                    reconnectAttempts = 0
                    delay(500)
                    doConnect()
                }
            }
        }
    }

    fun connect(host: String, port: Int) {
        currentHost = host
        currentPort = port
        shouldReconnect = true
        reconnectAttempts = 0
        doConnect()
    }

    private fun doConnect() {
        scope.launch {
            try {
                if (!_isNetworkAvailable.value) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return@launch
                }

                _connectionState.value = ConnectionState.CONNECTING
                socket?.close()
                val sock = Socket()
                sock.connect(InetSocketAddress(currentHost, currentPort), 10000)
                sock.soTimeout = 0
                sock.tcpNoDelay = true
                socket = sock
                output = sock.getOutputStream()
                input = sock.getInputStream()
                performHandshake()
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                startReading()

                val authMsg = JSONObject().apply {
                    put("type", "auth")
                    put("pin", AUTH_PIN_OVERRIDE ?: DEFAULT_AUTH_PIN)
                }
                val authFrame = encodeFrame(authMsg.toString().toByteArray())
                synchronized(output!!) {
                    output?.write(authFrame)
                    output?.flush()
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _messages.emit(WsMessage(type = "error", data = "Connection failed: ${e.message}"))
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        if (!_isNetworkAvailable.value) return

        reconnectAttempts++
        val exponentialDelay = BASE_DELAY_MS * 2.0.pow(reconnectAttempts - 1).toLong()
        val jitter = Random.nextLong(0, exponentialDelay / 4)
        val delayMs = min(exponentialDelay + jitter, MAX_DELAY_MS)

        scope.launch {
            delay(delayMs)
            if (shouldReconnect && _connectionState.value != ConnectionState.CONNECTED && _isNetworkAvailable.value) {
                doConnect()
            }
        }
    }

    fun disconnect() {
        shouldReconnect = false
        readerJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun destroy() {
        disconnect()
        networkMonitor?.stop()
        scope.cancel()
    }

    private fun performHandshake() {
        val key = Base64.getEncoder().encodeToString(ByteArray(16).also { kotlin.random.Random.Default.nextBytes(it) })
        val request = buildString {
            append("GET / HTTP/1.1\r\n")
            append("Host: $currentHost:$currentPort\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: $key\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            append("Sec-WebSocket-Protocol: mimocode-v1\r\n")
            append("\r\n")
        }
        output!!.write(request.toByteArray())
        output!!.flush()

        val response = ByteArray(4096)
        var totalRead = 0
        while (totalRead < response.size) {
            val read = input!!.read(response, totalRead, response.size - totalRead)
            if (read == -1) break
            totalRead += read
            if (String(response, 0, totalRead).contains("\r\n\r\n")) break
        }
    }

    private fun startReading() {
        readerJob = scope.launch {
            val buffer = ByteArray(131072)
            var leftover = ByteArray(0)
            while (isActive) {
                try {
                    val read = input!!.read(buffer)
                    if (read == -1) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        scheduleReconnect()
                        break
                    }
                    val data = leftover + buffer.copyOf(read)
                    var offset = 0
                    while (offset < data.size) {
                        val (message, consumed) = decodeFrame(data, offset)
                        if (consumed == 0) {
                            leftover = data.copyOfRange(offset, data.size)
                            break
                        }
                        offset = consumed
                        leftover = ByteArray(0)
                        if (message != null) {
                            try {
                                val json = JSONObject(message)
                                val entriesArray = json.optJSONArray("entries")
                                val entriesList = if (entriesArray != null) {
                                    val list = mutableListOf<Map<String, Any>>()
                                    for (i in 0 until entriesArray.length()) {
                                        val obj = entriesArray.getJSONObject(i)
                                        val map = mutableMapOf<String, Any>()
                                        for (key in obj.keys()) {
                                            map[key] = obj.get(key)
                                        }
                                        list.add(map)
                                    }
                                    list
                                } else null
                                _messages.emit(WsMessage(
                                    type = json.optString("type", "unknown"),
                                    id = json.optString("id", null),
                                    data = json.opt("data"),
                                    path = json.optString("path", null),
                                    entries = entriesList,
                                    exitCode = if (json.has("exit_code")) json.optInt("exit_code") else null,
                                    error = if (json.has("error")) json.optString("error", null) else null,
                                    command = json.optString("command", null),
                                    filename = json.optString("filename", null),
                                    instance_id = json.optString("instance_id", null)
                                ))
                            } catch (e: Exception) {
                                _messages.emit(WsMessage(type = "raw", data = message))
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        _connectionState.value = ConnectionState.ERROR
                        scheduleReconnect()
                    }
                    break
                }
            }
        }
    }

    fun sendMessage(message: WsMessage) {
        scope.launch {
            try {
                if (_connectionState.value != ConnectionState.CONNECTED) return@launch
                val payload = message.toJson().toByteArray()
                val frame = encodeFrame(payload)
                synchronized(output!!) {
                    output?.write(frame)
                    output?.flush()
                }
            } catch (e: Exception) {
                _messages.emit(WsMessage(type = "error", data = "Send failed: ${e.message}"))
            }
        }
    }

    fun nextId(): String = "msg_${++requestId}_${System.currentTimeMillis()}"

    fun sendChat(prompt: String): String {
        val id = nextId()
        sendMessage(WsMessage(type = "chat", id = id, prompt = prompt))
        return id
    }

    fun sendExecute(command: String, cwd: String? = null): String {
        val id = nextId()
        sendMessage(WsMessage(type = "execute", id = id, command = command))
        return id
    }

    fun sendReadFile(path: String): String {
        val id = nextId()
        sendMessage(WsMessage(type = "read_file", id = id, path = path))
        return id
    }

    fun sendWriteFile(path: String, content: String): String {
        val id = nextId()
        sendMessage(WsMessage(type = "write_file", id = id, path = path, content = content))
        return id
    }

    fun sendDeleteFile(path: String): String {
        val id = nextId()
        sendMessage(WsMessage(type = "delete_file", id = id, path = path))
        return id
    }

    fun sendListDir(path: String = "."): String {
        val id = nextId()
        sendMessage(WsMessage(type = "list_dir", id = id, path = path))
        return id
    }

    fun sendDeviceList(): String {
        val id = nextId()
        sendMessage(WsMessage(type = "device_list", id = id))
        return id
    }

    fun sendDeviceCommand(serial: String, command: String, action: String = "shell"): String {
        val id = nextId()
        val obj = org.json.JSONObject().apply {
            put("type", "device_command")
            put("id", id)
            put("serial", serial)
            put("command", command)
            put("action", action)
        }
        sendMessage(WsMessage(type = "device_command", id = id, data = obj.toString()))
        return id
    }

    fun sendDeviceInput(serial: String, inputType: String, value: String): String {
        val id = nextId()
        val obj = org.json.JSONObject().apply {
            put("type", "device_command")
            put("id", id)
            put("serial", serial)
            put("action", "input")
            put("input_type", inputType)
            put("value", value)
        }
        sendMessage(WsMessage(type = "device_command", id = id, data = obj.toString()))
        return id
    }

    fun sendDeviceInstall(serial: String, apkPath: String): String {
        val id = nextId()
        val obj = org.json.JSONObject().apply {
            put("type", "device_command")
            put("id", id)
            put("serial", serial)
            put("action", "install")
            put("apk_path", apkPath)
        }
        sendMessage(WsMessage(type = "device_command", id = id, data = obj.toString()))
        return id
    }

    fun sendAdbConfigure(serial: String): String {
        val id = nextId()
        sendMessage(WsMessage(type = "adb_configure", id = id, data = org.json.JSONObject().apply {
            put("serial", serial)
        }.toString()))
        return id
    }

    private fun encodeFrame(data: ByteArray): ByteArray {
        val frame = mutableListOf<Byte>()
        frame.add(0x81.toByte())
        if (data.size < 126) {
            frame.add((0x80 or data.size).toByte())
        } else if (data.size < 65536) {
            frame.add((0x80 or 126).toByte())
            frame.addAll(byteArrayOf((data.size shr 8).toByte(), data.size.toByte()).toList())
        } else {
            frame.add((0x80 or 127).toByte())
            for (i in 7 downTo 0) frame.add(((data.size shr (i * 8)) and 0xFF).toByte())
        }
        val mask = ByteArray(4).also { kotlin.random.Random.Default.nextBytes(it) }
        frame.addAll(mask.toList())
        frame.addAll(data.mapIndexed { i, b -> (b.toInt().xor(mask[i % 4].toInt()) and 0xFF).toByte() })
        return frame.toByteArray()
    }

    private fun decodeFrame(data: ByteArray, startOffset: Int = 0): Pair<String?, Int> {
        if (data.size - startOffset < 2) return Pair(null, startOffset)
        val b1 = data[startOffset].toInt() and 0xFF
        val b2 = data[startOffset + 1].toInt() and 0xFF
        val opcode = b1 and 0x0F
        if (opcode == 0x08) return Pair(null, data.size)
        val length = b2 and 0x7F
        var offset = startOffset + 2
        val actualLength = when (length) {
            126 -> {
                if (data.size - offset < 2) return Pair(null, startOffset)
                val l = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                offset += 2; l
            }
            127 -> {
                if (data.size - offset < 8) return Pair(null, startOffset)
                var l = 0L
                for (i in 0 until 8) l = (l shl 8) or (data[offset + i].toLong() and 0xFF)
                offset += 8; l.toInt()
            }
            else -> length
        }
        if (data.size - offset < actualLength) return Pair(null, startOffset)
        val payload = String(data, offset, actualLength)
        return Pair(payload, offset + actualLength)
    }
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
