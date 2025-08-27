package com.lulan.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanOnlyFilter(private val context: Context) {
    private val _lanOnly = MutableStateFlow(true)
    val lanOnly = _lanOnly.asStateFlow()

    fun setLanOnly(enabled: Boolean) { _lanOnly.value = enabled }

    fun allowAddress(ip: String): Boolean {
        if (!_lanOnly.value) return true
        return LanIpUtil.isRfc1918(ip)
    }

    fun hasLan(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
