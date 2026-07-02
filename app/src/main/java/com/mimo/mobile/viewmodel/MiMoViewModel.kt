package com.mimo.mobile.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.network.WebSocketClient
import com.mimo.mobile.network.WsMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mimo_settings")

data class AppState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val serverHost: String = "127.0.0.1",
    val serverPort: String = "9123",
    val isSplashDone: Boolean = false,
    val lastError: String? = null,
    val connectedAt: Long? = null,
    val messageCount: Int = 0,
    val autoConnect: Boolean = true,
    val screenStreaming: Boolean = true,
    val hapticFeedback: Boolean = true,
    val autoScroll: Boolean = true,
    val codeHighlighting: Boolean = true,
    val darkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val lowLatencyMode: Boolean = true,
    val compactUI: Boolean = false,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val autoReconnect: Boolean = true,
    val maxReconnectAttempts: Int = 5,
    val connectionTimeout: Int = 10,
    val sendBufferSize: Int = 4096,
    val maxMessageLength: Int = 65536,
    val enableCache: Boolean = true,
    val cacheExpiry: Int = 300,
    val telemetryEnabled: Boolean = false,
    val crashReporting: Boolean = false,
    val experimentalFeatures: Boolean = false,
    val betaUpdates: Boolean = false
)

data class ChatInstance(
    val id: String,
    val name: String,
    val messages: List<ChatMsg> = emptyList(),
    val terminalOutput: List<String> = emptyList(),
    val isProcessing: Boolean = false
)

data class ChatMsg(
    val id: String,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val instanceId: String = "default"
)

class MiMoViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore
    private val client = WebSocketClient()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    val messages: SharedFlow<WsMessage> = client.messages
    val isNetworkAvailable: StateFlow<Boolean> = client.isNetworkAvailable

    private val _instances = mutableStateListOf(
        ChatInstance("default", "Main")
    )
    val instances: List<ChatInstance> get() = _instances

    private var activeInstanceId = "default"

    private val _activeInstanceId = MutableStateFlow("default")
    val activeInstanceIdFlow: StateFlow<String> = _activeInstanceId.asStateFlow()

    init {
        client.initNetworkMonitor(application)

        viewModelScope.launch {
            dataStore.data.map { prefs ->
                val host = prefs[HOST_KEY] ?: "127.0.0.1"
                val port = prefs[PORT_KEY] ?: "9123"
                val pin = prefs[PIN_KEY] ?: "MIMO2026"
                Triple(host, port, pin)
            }.collect { (host, port, pin) ->
                _state.update { it.copy(serverHost = host, serverPort = port) }
                WebSocketClient.AUTH_PIN_OVERRIDE = pin
            }
        }

        viewModelScope.launch {
            dataStore.data.map { prefs ->
                AppState(
                    autoConnect = prefs[AUTO_CONNECT_KEY] ?: true,
                    screenStreaming = prefs[SCREEN_STREAMING_KEY] ?: true,
                    hapticFeedback = prefs[HAPTIC_FEEDBACK_KEY] ?: true,
                    autoScroll = prefs[AUTO_SCROLL_KEY] ?: true,
                    codeHighlighting = prefs[CODE_HIGHLIGHTING_KEY] ?: true,
                    darkMode = prefs[DARK_MODE_KEY] ?: true,
                    notificationsEnabled = prefs[NOTIFICATIONS_KEY] ?: true,
                    soundEnabled = prefs[SOUND_KEY] ?: false,
                    vibrationEnabled = prefs[VIBRATION_KEY] ?: true,
                    keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: false,
                    lowLatencyMode = prefs[LOW_LATENCY_KEY] ?: true,
                    compactUI = prefs[COMPACT_UI_KEY] ?: false,
                    showLineNumbers = prefs[LINE_NUMBERS_KEY] ?: true,
                    wordWrap = prefs[WORD_WRAP_KEY] ?: true,
                    autoReconnect = prefs[AUTO_RECONNECT_KEY] ?: true,
                    maxReconnectAttempts = prefs[MAX_RECONNECT_KEY] ?: 5,
                    connectionTimeout = prefs[CONN_TIMEOUT_KEY] ?: 10,
                    sendBufferSize = prefs[SEND_BUFFER_KEY] ?: 4096,
                    maxMessageLength = prefs[MAX_MSG_LEN_KEY] ?: 65536,
                    enableCache = prefs[CACHE_KEY] ?: true,
                    cacheExpiry = prefs[CACHE_EXPIRY_KEY] ?: 300,
                    telemetryEnabled = prefs[TELEMETRY_KEY] ?: false,
                    crashReporting = prefs[CRASH_REPORT_KEY] ?: false,
                    experimentalFeatures = prefs[EXPERIMENTAL_KEY] ?: false,
                    betaUpdates = prefs[BETA_UPDATES_KEY] ?: false
                )
            }.collect { saved ->
                _state.update {
                    it.copy(
                        autoConnect = saved.autoConnect,
                        screenStreaming = saved.screenStreaming,
                        hapticFeedback = saved.hapticFeedback,
                        autoScroll = saved.autoScroll,
                        codeHighlighting = saved.codeHighlighting,
                        darkMode = saved.darkMode,
                        notificationsEnabled = saved.notificationsEnabled,
                        soundEnabled = saved.soundEnabled,
                        vibrationEnabled = saved.vibrationEnabled,
                        keepScreenOn = saved.keepScreenOn,
                        lowLatencyMode = saved.lowLatencyMode,
                        compactUI = saved.compactUI,
                        showLineNumbers = saved.showLineNumbers,
                        wordWrap = saved.wordWrap,
                        autoReconnect = saved.autoReconnect,
                        maxReconnectAttempts = saved.maxReconnectAttempts,
                        connectionTimeout = saved.connectionTimeout,
                        sendBufferSize = saved.sendBufferSize,
                        maxMessageLength = saved.maxMessageLength,
                        enableCache = saved.enableCache,
                        cacheExpiry = saved.cacheExpiry,
                        telemetryEnabled = saved.telemetryEnabled,
                        crashReporting = saved.crashReporting,
                        experimentalFeatures = saved.experimentalFeatures,
                        betaUpdates = saved.betaUpdates
                    )
                }
            }
        }

        viewModelScope.launch {
            dataStore.data.map { prefs ->
                prefs[MESSAGES_KEY] ?: "[]"
            }.collect { json ->
                try {
                    val arr = JSONArray(json)
                    val savedMessages = mutableListOf<ChatMsg>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        savedMessages.add(ChatMsg(
                            id = obj.getString("id"),
                            role = obj.getString("role"),
                            content = obj.getString("content"),
                            isStreaming = false,
                            instanceId = obj.optString("instanceId", "default")
                        ))
                    }
                    if (savedMessages.isNotEmpty()) {
                        val defaultInst = _instances[0]
                        _instances[0] = defaultInst.copy(messages = savedMessages)
                    }
                } catch (_: Exception) {}
            }
        }

        viewModelScope.launch {
            client.connectionState.collect { connState ->
                _state.update {
                    it.copy(
                        connectionState = connState,
                        lastError = if (connState == ConnectionState.ERROR) "Connection failed" else null,
                        connectedAt = if (connState == ConnectionState.CONNECTED) System.currentTimeMillis() else it.connectedAt
                    )
                }
            }
        }

        viewModelScope.launch {
            client.messages.collect { msg ->
                val instId = msg.instance_id ?: "default"
                when (msg.type) {
                    "chat_start" -> {
                        val idx = _instances.indexOfFirst { it.id == instId }
                        if (idx >= 0) {
                            val inst = _instances[idx]
                            _instances[idx] = inst.copy(
                                isProcessing = true,
                                messages = inst.messages + ChatMsg(
                                    id = msg.id ?: "", role = "assistant", content = "",
                                    isStreaming = true, instanceId = instId
                                )
                            )
                        }
                    }
                    "chat_chunk" -> {
                        val idx = _instances.indexOfFirst { it.id == instId }
                        if (idx >= 0) {
                            val chunk = msg.data?.toString() ?: ""
                            val inst = _instances[idx]
                            val updatedMessages = inst.messages.map { m ->
                                if (m.id == msg.id) {
                                    val newContent = if (m.content.isEmpty()) chunk else m.content + "\n" + chunk
                                    m.copy(content = newContent)
                                } else m
                            }
                            _instances[idx] = inst.copy(messages = updatedMessages)
                        }
                    }
                    "terminal_chunk" -> {
                        val idx = _instances.indexOfFirst { it.id == instId }
                        if (idx >= 0) {
                            val chunk = msg.data?.toString() ?: ""
                            val inst = _instances[idx]
                            _instances[idx] = inst.copy(terminalOutput = inst.terminalOutput + chunk)
                        }
                    }
                    "chat_end" -> {
                        val idx = _instances.indexOfFirst { it.id == instId }
                        if (idx >= 0) {
                            val inst = _instances[idx]
                            val updatedMessages = inst.messages.map { m ->
                                if (m.id == msg.id) m.copy(isStreaming = false) else m
                            }
                            _instances[idx] = inst.copy(messages = updatedMessages, isProcessing = false)
                            saveMessages()
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            client.messages.collect { msg ->
                if (msg.type == "chat_end") {
                    kotlinx.coroutines.delay(300)
                    connect()
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            connect()
        }
    }

    fun switchInstance(instanceId: String) {
        activeInstanceId = instanceId
        _activeInstanceId.value = instanceId
    }

    fun addInstance(name: String) {
        val id = "inst_${System.currentTimeMillis()}"
        _instances.add(ChatInstance(id, name))
    }

    fun removeInstance(instanceId: String) {
        if (instanceId == "default") return
        val idx = _instances.indexOfFirst { it.id == instanceId }
        if (idx >= 0) _instances.removeAt(idx)
        if (activeInstanceId == instanceId) {
            activeInstanceId = "default"
            _activeInstanceId.value = "default"
        }
    }

    fun connect() {
        val s = _state.value
        if (!s.autoConnect && s.connectionState == ConnectionState.DISCONNECTED) return
        if (s.serverHost.isEmpty()) return
        val port = s.serverPort.toIntOrNull() ?: 9123
        client.connect(s.serverHost, port)
    }

    fun disconnect() { client.disconnect() }
    fun reconnect() { disconnect(); connect() }

    fun updateHost(host: String) {
        _state.update { it.copy(serverHost = host) }
        viewModelScope.launch { dataStore.edit { it[HOST_KEY] = host } }
    }

    fun updatePort(port: String) {
        _state.update { it.copy(serverPort = port) }
        viewModelScope.launch { dataStore.edit { it[PORT_KEY] = port } }
    }

    fun updatePin(pin: String) {
        viewModelScope.launch { dataStore.edit { it[PIN_KEY] = pin } }
    }

    fun updateTheme(darkMode: Boolean) {
        viewModelScope.launch { dataStore.edit { it[DARK_MODE_KEY] = darkMode } }
    }

    fun updateToggle(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
        _state.update {
            when (key) {
                AUTO_CONNECT_KEY -> it.copy(autoConnect = value)
                SCREEN_STREAMING_KEY -> it.copy(screenStreaming = value)
                HAPTIC_FEEDBACK_KEY -> it.copy(hapticFeedback = value)
                AUTO_SCROLL_KEY -> it.copy(autoScroll = value)
                CODE_HIGHLIGHTING_KEY -> it.copy(codeHighlighting = value)
                DARK_MODE_KEY -> it.copy(darkMode = value)
                NOTIFICATIONS_KEY -> it.copy(notificationsEnabled = value)
                SOUND_KEY -> it.copy(soundEnabled = value)
                VIBRATION_KEY -> it.copy(vibrationEnabled = value)
                KEEP_SCREEN_ON_KEY -> it.copy(keepScreenOn = value)
                LOW_LATENCY_KEY -> it.copy(lowLatencyMode = value)
                COMPACT_UI_KEY -> it.copy(compactUI = value)
                LINE_NUMBERS_KEY -> it.copy(showLineNumbers = value)
                WORD_WRAP_KEY -> it.copy(wordWrap = value)
                AUTO_RECONNECT_KEY -> it.copy(autoReconnect = value)
                CACHE_KEY -> it.copy(enableCache = value)
                TELEMETRY_KEY -> it.copy(telemetryEnabled = value)
                CRASH_REPORT_KEY -> it.copy(crashReporting = value)
                EXPERIMENTAL_KEY -> it.copy(experimentalFeatures = value)
                BETA_UPDATES_KEY -> it.copy(betaUpdates = value)
                else -> it
            }
        }
    }

    fun sendChat(prompt: String, instanceId: String = activeInstanceId): String {
        val instIdx = _instances.indexOfFirst { it.id == instanceId }
        if (instIdx < 0) return ""

        val userMsgId = "msg_${System.currentTimeMillis()}_user"
        _instances[instIdx] = _instances[instIdx].copy(
            isProcessing = true,
            messages = _instances[instIdx].messages + ChatMsg(
                id = userMsgId, role = "user", content = prompt, instanceId = instanceId
            )
        )

        val id = client.nextId()
        client.sendMessage(WsMessage(type = "chat", id = id, prompt = prompt, instance_id = instanceId))
        return id
    }

    fun sendExecute(command: String, cwd: String? = null): String = client.sendExecute(command, cwd)
    fun sendReadFile(path: String): String = client.sendReadFile(path)
    fun sendWriteFile(path: String, content: String): String = client.sendWriteFile(path, content)
    fun sendDeleteFile(path: String): String = client.sendDeleteFile(path)
    fun sendListDir(path: String = "."): String = client.sendListDir(path)
    fun sendBuildProgress(): String {
        val id = client.nextId()
        client.sendMessage(WsMessage(type = "build_progress", id = id))
        return id
    }

    fun sendDeviceList(): String = client.sendDeviceList()
    fun sendDeviceCommand(serial: String, command: String, action: String = "shell"): String = client.sendDeviceCommand(serial, command, action)
    fun sendDeviceInput(serial: String, inputType: String, value: String): String = client.sendDeviceInput(serial, inputType, value)
    fun sendDeviceInstall(serial: String, apkPath: String): String = client.sendDeviceInstall(serial, apkPath)
    fun sendAdbConfigure(serial: String): String = client.sendAdbConfigure(serial)
    fun sendMessage(msg: WsMessage) { client.sendMessage(msg) }
    fun nextId(): String = client.nextId()

    fun incrementMessageCount() {
        _state.update { it.copy(messageCount = it.messageCount + 1) }
    }

    fun dismissSplash() {
        _state.update { it.copy(isSplashDone = true) }
    }

    private fun saveMessages() {
        viewModelScope.launch {
            try {
                val defaultInst = _instances.find { it.id == "default" } ?: return@launch
                val arr = JSONArray()
                defaultInst.messages.filter { !it.isStreaming }.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("id", msg.id)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("instanceId", msg.instanceId)
                    })
                }
                dataStore.edit { it[MESSAGES_KEY] = arr.toString() }
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.destroy()
    }

    companion object {
        val HOST_KEY = stringPreferencesKey("server_host")
        val PORT_KEY = stringPreferencesKey("server_port")
        val PIN_KEY = stringPreferencesKey("auth_pin")
        val MESSAGES_KEY = stringPreferencesKey("chat_messages")
        val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect")
        val SCREEN_STREAMING_KEY = booleanPreferencesKey("screen_streaming")
        val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")
        val AUTO_SCROLL_KEY = booleanPreferencesKey("auto_scroll")
        val CODE_HIGHLIGHTING_KEY = booleanPreferencesKey("code_highlighting")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        val SOUND_KEY = booleanPreferencesKey("sound")
        val VIBRATION_KEY = booleanPreferencesKey("vibration")
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        val LOW_LATENCY_KEY = booleanPreferencesKey("low_latency")
        val COMPACT_UI_KEY = booleanPreferencesKey("compact_ui")
        val LINE_NUMBERS_KEY = booleanPreferencesKey("line_numbers")
        val WORD_WRAP_KEY = booleanPreferencesKey("word_wrap")
        val AUTO_RECONNECT_KEY = booleanPreferencesKey("auto_reconnect")
        val MAX_RECONNECT_KEY = intPreferencesKey("max_reconnect")
        val CONN_TIMEOUT_KEY = intPreferencesKey("conn_timeout")
        val SEND_BUFFER_KEY = intPreferencesKey("send_buffer")
        val MAX_MSG_LEN_KEY = intPreferencesKey("max_msg_len")
        val CACHE_KEY = booleanPreferencesKey("enable_cache")
        val CACHE_EXPIRY_KEY = intPreferencesKey("cache_expiry")
        val TELEMETRY_KEY = booleanPreferencesKey("telemetry")
        val CRASH_REPORT_KEY = booleanPreferencesKey("crash_reporting")
        val EXPERIMENTAL_KEY = booleanPreferencesKey("experimental")
        val BETA_UPDATES_KEY = booleanPreferencesKey("beta_updates")
    }
}
