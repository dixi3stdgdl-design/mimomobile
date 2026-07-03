package com.mimo.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AdbHttpClient {

    private var baseUrl = ""

    fun configure(host: String, httpPort: Int = 8080) {
        baseUrl = "http://$host:$httpPort"
    }

    suspend fun getDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        val response = httpGet("/api/adb/devices")
        val json = JSONObject(response)
        val arr = json.getJSONArray("devices")
        val devices = mutableListOf<AdbDevice>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            devices.add(AdbDevice(
                serial = obj.getString("serial"),
                state = obj.getString("state"),
                model = obj.optString("model", "")
            ))
        }
        devices
    }

    suspend fun execCommand(serial: String, command: String, action: String = "shell"): AdbResult = withContext(Dispatchers.IO) {
        val params = "serial=${enc(serial)}&command=${enc(command)}&action=${enc(action)}"
        val response = httpGet("/api/adb/exec?$params")
        val json = JSONObject(response)
        AdbResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = json.optInt("exit_code", -1)
        )
    }

    suspend fun sendInput(serial: String, inputType: String, value: String): AdbResult = withContext(Dispatchers.IO) {
        val params = "serial=${enc(serial)}&action=input&input_type=${enc(inputType)}&value=${enc(value)}"
        val response = httpGet("/api/adb/exec?$params")
        val json = JSONObject(response)
        AdbResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = json.optInt("exit_code", -1)
        )
    }

    suspend fun installApk(serial: String, apkPath: String): AdbResult = withContext(Dispatchers.IO) {
        val params = "serial=${enc(serial)}&command=${enc(apkPath)}&action=install"
        val response = httpGet("/api/adb/exec?$params")
        val json = JSONObject(response)
        AdbResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = json.optInt("exit_code", -1)
        )
    }

    suspend fun pushFile(serial: String, localPath: String, remotePath: String): AdbResult = withContext(Dispatchers.IO) {
        val params = "serial=${enc(serial)}&command=${enc("$localPath $remotePath")}&action=push"
        val response = httpGet("/api/adb/exec?$params")
        val json = JSONObject(response)
        AdbResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = json.optInt("exit_code", -1)
        )
    }

    suspend fun pullFile(serial: String, remotePath: String, localPath: String): AdbResult = withContext(Dispatchers.IO) {
        val params = "serial=${enc(serial)}&command=${enc("$remotePath $localPath")}&action=pull"
        val response = httpGet("/api/adb/exec?$params")
        val json = JSONObject(response)
        AdbResult(
            stdout = json.optString("stdout", ""),
            stderr = json.optString("stderr", ""),
            exitCode = json.optInt("exit_code", -1)
        )
    }

    suspend fun connectDevice(ip: String, port: String = "5555"): String = withContext(Dispatchers.IO) {
        val params = "ip=${enc(ip)}&port=${enc(port)}"
        val response = httpGet("/api/adb/connect?$params")
        val json = JSONObject(response)
        json.optString("stdout", json.optString("error", "Unknown error"))
    }

    private fun httpGet(path: String): String {
        val url = URL("$baseUrl$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 30000
        }
        val response = if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Connection failed"
            throw Exception("HTTP ${conn.responseCode}: $error")
        }
        conn.disconnect()
        return response
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

data class AdbDevice(
    val serial: String,
    val state: String,
    val model: String
)

data class AdbResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)
