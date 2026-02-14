package com.example.helios_alarm_clock.util

import java.net.Inet4Address
import java.net.NetworkInterface

fun getLocalIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    } catch (_: Exception) {
        null
    }
}
