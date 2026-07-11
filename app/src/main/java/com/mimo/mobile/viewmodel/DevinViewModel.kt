package com.mimo.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class DevinSession(
    val sessionId: String,
    val task: String,
    val status: String,
    val output: List<String>,
    val createdAt: Long
)

sealed class DevinUiState {
    object Idle : DevinUiState()
    object Loading : DevinUiState()
    data class Success(val sessions: List<DevinSession>) : DevinUiState()
    data class Error(val message: String) : DevinUiState()
}

class DevinViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DevinUiState>(DevinUiState.Idle)
    val uiState: StateFlow<DevinUiState> = _uiState

    private val _currentSession = MutableStateFlow<DevinSession?>(null)
    val currentSession: StateFlow<DevinSession?> = _currentSession

    private var serverUrl: String = ""

    fun setServerUrl(url: String) {
        serverUrl = url.trimEnd('/')
    }

    fun executeTask(task: String, branch: String = "main", description: String = "") {
        viewModelScope.launch {
            _uiState.value = DevinUiState.Loading
            try {
                val url = "$serverUrl/api/devin/execute"
                val body = JSONObject().apply {
                    put("task", task)
                    put("branch", branch)
                    put("description", description)
                }
                val response = makePostRequest(url, body.toString())
                val json = JSONObject(response)
                
                if (json.has("session_id")) {
                    val session = DevinSession(
                        sessionId = json.getString("session_id"),
                        task = task,
                        status = "running",
                        output = json.optJSONArray("output")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                        createdAt = System.currentTimeMillis()
                    )
                    _currentSession.value = session
                    pollSessionStatus(session.sessionId)
                } else {
                    _uiState.value = DevinUiState.Error(json.optString("message", "Unknown error"))
                }
            } catch (e: Exception) {
                _uiState.value = DevinUiState.Error(e.message ?: "Connection failed")
            }
        }
    }

    private fun pollSessionStatus(sessionId: String) {
        viewModelScope.launch {
            while (true) {
                try {
                    val url = "$serverUrl/api/devin/status?session_id=$sessionId"
                    val response = makeRequest(url)
                    val json = JSONObject(response)
                    
                    val session = DevinSession(
                        sessionId = json.getString("session_id"),
                        task = json.getString("task"),
                        status = json.getString("status"),
                        output = json.optJSONArray("output")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList(),
                        createdAt = json.optLong("created_at", System.currentTimeMillis())
                    )
                    
                    _currentSession.value = session
                    
                    if (session.status == "completed" || session.status == "failed" || session.status == "cancelled") {
                        _uiState.value = DevinUiState.Success(listOf(session))
                        break
                    }
                    
                    kotlinx.coroutines.delay(2000) // Poll every 2 seconds
                } catch (e: Exception) {
                    // Continue polling on error
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
    }

    fun cancelTask(sessionId: String) {
        viewModelScope.launch {
            try {
                val url = "$serverUrl/api/devin/cancel"
                val body = JSONObject().apply {
                    put("session_id", sessionId)
                }
                makePostRequest(url, body.toString())
                _currentSession.value = _currentSession.value?.copy(status = "cancelled")
            } catch (e: Exception) {
                _uiState.value = DevinUiState.Error(e.message ?: "Failed to cancel")
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = DevinUiState.Loading
            try {
                val url = "$serverUrl/api/devin/status"
                val response = makeRequest(url)
                val json = JSONObject(response)
                val sessionsArray = json.optJSONArray("sessions")
                
                val sessions = if (sessionsArray != null) {
                    (0 until sessionsArray.length()).map { i ->
                        val s = sessionsArray.getJSONObject(i)
                        DevinSession(
                            sessionId = s.getString("session_id"),
                            task = s.getString("task"),
                            status = s.getString("status"),
                            output = s.optJSONArray("output")?.let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            } ?: emptyList(),
                            createdAt = s.optLong("created_at", System.currentTimeMillis())
                        )
                    }
                } else {
                    emptyList()
                }
                
                _uiState.value = DevinUiState.Success(sessions)
            } catch (e: Exception) {
                _uiState.value = DevinUiState.Error(e.message ?: "Failed to load sessions")
            }
        }
    }

    private fun makeRequest(urlStr: String): String {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        
        return try {
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun makePostRequest(urlStr: String, jsonBody: String): String {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.doOutput = true
        
        return try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(jsonBody)
            }
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}
