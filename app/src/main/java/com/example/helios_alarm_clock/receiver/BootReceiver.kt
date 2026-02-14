package com.example.helios_alarm_clock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.helios_alarm_clock.service.KtorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            KtorService.start(context)
        }
    }
}
