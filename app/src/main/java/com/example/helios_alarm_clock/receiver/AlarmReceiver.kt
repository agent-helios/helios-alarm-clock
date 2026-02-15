package com.example.helios_alarm_clock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.helios_alarm_clock.service.AlarmRingService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"

        // Start foreground service with full-screen intent notification.
        // Direct startActivity() from a BroadcastReceiver is blocked on Android 10+.
        AlarmRingService.start(context, alarmId, label)
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
    }
}
