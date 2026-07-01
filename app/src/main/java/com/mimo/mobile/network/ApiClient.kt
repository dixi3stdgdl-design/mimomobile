package com.mimo.mobile.network

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mimo.mobile.viewmodel.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class ApiProvider(val displayName: String, val defaultModel: String, val defaultEndpoint: String) {
    OPENAI("OpenAI", "gpt-4o", "https://api.openai.com/v1/chat/completions"),
    CLAUDE("Claude (Anthropic)", "claude-sonnet-4-20250514", "https://api.anthropic.com/v1/messages"),
    MIMO("MiMo Auto", "mimo-auto", ""),
    CUSTOM("Custom", "", "")
}

data class ApiConfig(
    val provider: ApiProvider = ApiProvider.OPENAI,
    val apiKey: String = "",
    val model: String = ApiProvider.OPENAI.defaultModel,
    val endpoint: String = ApiProvider.OPENAI.defaultEndpoint
)

class ApiClient(private val app: Application) {

    companion object {
        val PROVIDER_KEY = stringPreferencesKey("api_provider")
        val API_KEY_KEY = stringPreferencesKey("api_key")
        val MODEL_KEY = stringPreferencesKey("api_model")
        val ENDPOINT_KEY = stringPreferencesKey("api_endpoint")
    }

    suspend fun loadConfig(): ApiConfig {
        val prefs = app.dataStore.data.first()
        val providerName = prefs[PROVIDER_KEY] ?: ApiProvider.OPENAI.name
        val provider = try { ApiProvider.valueOf(providerName) } catch (_: Exception) { ApiProvider.OPENAI }
        return ApiConfig(
            provider = provider,
            apiKey = prefs[API_KEY_KEY] ?: "",
            model = prefs[MODEL_KEY] ?: provider.defaultModel,
            endpoint = prefs[ENDPOINT_KEY] ?: provider.defaultEndpoint
        )
    }

    suspend fun saveConfig(config: ApiConfig) {
        app.dataStore.edit { prefs ->
            prefs[PROVIDER_KEY] = config.provider.name
            prefs[API_KEY_KEY] = config.apiKey
            prefs[MODEL_KEY] = config.model
            prefs[ENDPOINT_KEY] = config.endpoint
        }
    }

    suspend fun sendMessage(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        config: ApiConfig? = null
    ): String = withContext(Dispatchers.IO) {
        val cfg = config ?: loadConfig()
        when (cfg.provider) {
            ApiProvider.OPENAI -> callOpenAI(prompt, history, cfg)
            ApiProvider.CLAUDE -> callClaude(prompt, history, cfg)
            ApiProvider.MIMO -> callMiMo(prompt, history, cfg)
            ApiProvider.CUSTOM -> callCustom(prompt, history, cfg)
        }
    }

    suspend fun sendMessageStreaming(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        config: ApiConfig? = null,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val cfg = config ?: loadConfig()
        when (cfg.provider) {
            ApiProvider.OPENAI -> callOpenAIStream(prompt, history, cfg, onChunk)
            ApiProvider.CLAUDE -> callClaudeStream(prompt, history, cfg, onChunk)
            ApiProvider.MIMO -> callMiMoStream(prompt, history, cfg, onChunk)
            ApiProvider.CUSTOM -> callCustomStream(prompt, history, cfg, onChunk)
        }
    }

    private fun callOpenAI(prompt: String, history: List<Pair<String, String>>, config: ApiConfig): String {
        val url = URL(config.endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
        }

        val messages = buildJSONArray {
            for ((role, content) in history) {
                put(JSONObject().put("role", role).put("content", content))
            }
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", 4096)

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val response = if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API error ${conn.responseCode}: $error")
        }

        conn.disconnect()
        val json = JSONObject(response)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun callOpenAIStream(prompt: String, history: List<Pair<String, String>>, config: ApiConfig, onChunk: (String) -> Unit): String {
        val url = URL(config.endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connectTimeout = 30000
            readTimeout = 120000
            doOutput = true
        }

        val messages = buildJSONArray {
            for ((role, content) in history) {
                put(JSONObject().put("role", role).put("content", content))
            }
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", 4096)
            .put("stream", true)

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val fullResponse = StringBuilder()
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            if (data == "[DONE]") break
            try {
                val chunk = JSONObject(data)
                val delta = chunk.getJSONArray("choices").getJSONObject(0).optJSONObject("delta")
                val content = delta?.optString("content", "") ?: ""
                if (content.isNotEmpty()) {
                    fullResponse.append(content)
                    onChunk(content)
                }
            } catch (_: Exception) {}
        }
        conn.disconnect()
        return fullResponse.toString()
    }

    private fun callClaude(prompt: String, history: List<Pair<String, String>>, config: ApiConfig): String {
        val url = URL(config.endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
        }

        val messages = buildJSONArray {
            for ((role, content) in history) {
                put(JSONObject().put("role", role).put("content", content))
            }
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("max_tokens", 4096)

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val response = if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API error ${conn.responseCode}: $error")
        }

        conn.disconnect()
        val json = JSONObject(response)
        return json.getJSONArray("content").getJSONObject(0).getString("text")
    }

    private fun callClaudeStream(prompt: String, history: List<Pair<String, String>>, config: ApiConfig, onChunk: (String) -> Unit): String {
        val url = URL(config.endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", config.apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            connectTimeout = 30000
            readTimeout = 120000
            doOutput = true
        }

        val messages = buildJSONArray {
            for ((role, content) in history) {
                put(JSONObject().put("role", role).put("content", content))
            }
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("max_tokens", 4096)
            .put("stream", true)

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val fullResponse = StringBuilder()
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            try {
                val event = JSONObject(data)
                if (event.getString("type") == "content_block_delta") {
                    val text = event.getJSONObject("delta").optString("text", "")
                    if (text.isNotEmpty()) {
                        fullResponse.append(text)
                        onChunk(text)
                    }
                }
            } catch (_: Exception) {}
        }
        conn.disconnect()
        return fullResponse.toString()
    }

    private fun callMiMo(prompt: String, history: List<Pair<String, String>>, config: ApiConfig): String {
        if (config.apiKey.isEmpty()) throw Exception("MiMo API key not configured")
        return callOpenAI(prompt, history, config.copy(endpoint = "https://api.mimocode.com/v1/chat/completions"))
    }

    private fun callMiMoStream(prompt: String, history: List<Pair<String, String>>, config: ApiConfig, onChunk: (String) -> Unit): String {
        if (config.apiKey.isEmpty()) throw Exception("MiMo API key not configured")
        return callOpenAIStream(prompt, history, config.copy(endpoint = "https://api.mimocode.com/v1/chat/completions"), onChunk)
    }

    private fun callCustom(prompt: String, history: List<Pair<String, String>>, config: ApiConfig): String {
        if (config.endpoint.isEmpty()) throw Exception("Custom endpoint not configured")
        return callOpenAI(prompt, history, config)
    }

    private fun callCustomStream(prompt: String, history: List<Pair<String, String>>, config: ApiConfig, onChunk: (String) -> Unit): String {
        if (config.endpoint.isEmpty()) throw Exception("Custom endpoint not configured")
        return callOpenAIStream(prompt, history, config, onChunk)
    }

    suspend fun testConnection(config: ApiConfig? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cfg = config ?: loadConfig()
            if (cfg.apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("API key not configured"))
            }
            val response = sendMessage("Say 'connected' in one word.", config = cfg)
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildJSONArray(block: JSONArray.() -> Unit): JSONArray {
        return JSONArray().apply(block)
    }
}
