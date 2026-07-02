package com.mimo.mobile.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        _isAvailable.value = caps?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        callback = NetworkMonitorCallback()
        try {
            connectivityManager.registerNetworkCallback(request, callback!!)
        } catch (_: Exception) {}
    }

    fun stop() {
        callback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        callback = null
    }

    private inner class NetworkMonitorCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isAvailable.value = false
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _isAvailable.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
