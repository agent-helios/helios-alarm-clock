package com.example.helios_alarm_clock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HeliosApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Server Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the HTTP server running in the background"
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(serviceChannel, alarmChannel))
    }

    companion object {
        const val CHANNEL_SERVICE = "ktor_service_channel"
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
