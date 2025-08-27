package com.lulan.app.util

import java.net.Inet4Address
import java.net.NetworkInterface

object LanIpUtil {
    /** Picks the first RFC1918 IPv4 address. */
    fun pickLanAddress(): String? {
        val ifaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        for (ni in ifaces) {
            if (!ni.isUp || ni.isLoopback) continue
            for (addr in ni.inetAddresses.toList()) {
                if (addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (isRfc1918(ip)) return ip
                }
            }
        }
        return null
    }

    fun isRfc1918(ip: String): Boolean =
        ip.startsWith("10.") ||
        ip.startsWith("192.168.") ||
        ip.matches(Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\..*"))
}
