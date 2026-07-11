package com.mimo.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DevinNotification(
    val id: String,
    val title: String,
    val message: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

sealed class NotificationUiState {
    object Idle : NotificationUiState()
    data class Connected(val notifications: List<DevinNotification> = emptyList()) : NotificationUiState()
    data class Disconnected(val reason: String = "") : NotificationUiState()
}

class DevinNotificationService : ViewModel() {
    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState

    private val _notifications = MutableStateFlow<List<DevinNotification>>(emptyList())
    val notifications: StateFlow<List<DevinNotification>> = _notifications

    private val _newNotification = MutableSharedFlow<DevinNotification>()
    val newNotification: SharedFlow<DevinNotification> = _newNotification

    private var webSocket: WebSocket? = null
    private var serverUrl: String = ""
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    fun connect(host: String, port: Int) {
        serverUrl = "ws://$host:$port"
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$serverUrl")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("[DevinWS] Connected to $serverUrl")
                reconnectAttempts = 0
                _uiState.value = NotificationUiState.Connected()

                // Send auth message
                val authMsg = JSONObject().apply {
                    put("type", "auth")
                    put("service", "devin_notifications")
                }
                webSocket.send(authMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    handleWebSocketMessage(json)
                } catch (e: Exception) {
                    println("[DevinWS] Error parsing message: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handle binary messages if needed
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("[DevinWS] Closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("[DevinWS] Closed: $code $reason")
                _uiState.value = NotificationUiState.Disconnected(reason)
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("[DevinWS] Failure: ${t.message}")
                _uiState.value = NotificationUiState.Disconnected(t.message ?: "Connection failed")
                attemptReconnect()
            }
        })
    }

    private fun handleWebSocketMessage(json: JSONObject) {
        val type = json.optString("type", "")

        when (type) {
            "webhook" -> {
                // Webhook notification from server
                val event = json.optJSONObject("event")
                if (event != null) {
                    val source = event.optString("source", "")
                    val eventType = event.optString("event_type", "")
                    val payload = event.optJSONObject("payload")

                    if (source == "devin") {
                        val notification = DevinNotification(
                            id = event.optString("id", ""),
                            title = "Devin AI",
                            message = payload?.optString("message", eventType) ?: eventType,
                            status = payload?.optString("status", "update") ?: "update"
                        )
                        addNotification(notification)
                    }
                }
            }
            "devin_update" -> {
                // Direct Devin status update
                val sessionId = json.optString("session_id", "")
                val status = json.optString("status", "")
                val output = json.optString("output", "")

                val notification = DevinNotification(
                    id = sessionId,
                    title = "Devin Task Update",
                    message = output.ifEmpty { "Status: $status" },
                    status = status
                )
                addNotification(notification)
            }
            "ping" -> {
                // Respond to ping
                val pong = JSONObject().apply { put("type", "pong") }
                webSocket?.send(pong.toString())
            }
        }
    }

    private fun addNotification(notification: DevinNotification) {
        viewModelScope.launch {
            _notifications.value = listOf(notification) + _notifications.value
            _newNotification.emit(notification)

            // Keep only last 50 notifications
            if (_notifications.value.size > 50) {
                _notifications.value = _notifications.value.take(50)
            }
        }
    }

    private fun attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delay = reconnectAttempts * 2000L // Exponential backoff
            println("[DevinWS] Reconnecting in ${delay}ms (attempt $reconnectAttempts)")

            viewModelScope.launch {
                kotlinx.coroutines.delay(delay)
                connectWebSocket()
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _uiState.value = NotificationUiState.Disconnected("Manual disconnect")
    }

    fun markAsRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(read = true) else it
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
