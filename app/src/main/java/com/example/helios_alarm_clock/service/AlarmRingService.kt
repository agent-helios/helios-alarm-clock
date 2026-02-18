package com.example.helios_alarm_clock.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.helios_alarm_clock.HeliosApp
import com.example.helios_alarm_clock.R
import com.example.helios_alarm_clock.data.AlarmDao
import com.example.helios_alarm_clock.receiver.AlarmReceiver
import com.example.helios_alarm_clock.ui.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingService : Service() {

    @Inject lateinit var alarmDao: AlarmDao

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID) ?: ""
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"

        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(alarmId, label),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startSound()

        // Save as last alarm and delete from the database
        if (alarmId.isNotEmpty()) {
            scope.launch {
                try {
                    val alarm = alarmDao.getById(alarmId)
                    if (alarm != null) {
                        saveLastAlarm(alarm.hour, alarm.minute, alarm.label)
                    }
                    alarmDao.deleteById(alarmId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process alarm $alarmId", e)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(foregroundServiceType: Int) {
        Log.w(TAG, "Service timeout reached, stopping")
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }

    private fun buildNotification(alarmId: String, label: String): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarmId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmRingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HeliosApp.CHANNEL_ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Helios Alarm")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(0, "Dismiss", stopPi)
            .setOngoing(true)
            .build()
    }

    private fun saveLastAlarm(hour: Int, minute: Int, label: String) {
        val prefs = getSharedPreferences("last_alarm", Context.MODE_PRIVATE)
        val now = Calendar.getInstance()
        prefs.edit()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putString("label", label)
            .putLong("firedAt", now.timeInMillis)
            .apply()
    }

    companion object {
        private const val TAG = "AlarmRingService"
        const val NOTIFICATION_ID = 2
        private const val ACTION_STOP = "com.example.helios_alarm_clock.STOP_ALARM"

        fun start(context: Context, alarmId: String, label: String) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }
    }
}
