package com.example.helios_alarm_clock.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.helios_alarm_clock.HeliosApp
import com.example.helios_alarm_clock.R
import com.example.helios_alarm_clock.data.AlarmDao
import com.example.helios_alarm_clock.data.AlarmEntity
import com.example.helios_alarm_clock.ui.MainActivity
import com.example.helios_alarm_clock.util.AlarmScheduler
import com.example.helios_alarm_clock.util.getLocalIpAddress
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class KtorService : Service() {

    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var alarmScheduler: AlarmScheduler

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification())
        startServer()
        rescheduleAlarms()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        server?.stop(1000, 2000)
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "helios::ktor_service"
        ).apply { acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun rescheduleAlarms() {
        serviceScope.launch {
            val now = System.currentTimeMillis()
            for (alarm in alarmDao.getAll()) {
                if (alarm.triggerTimeMillis > now) {
                    alarmScheduler.schedule(alarm)
                } else {
                    alarmDao.deleteById(alarm.id)
                }
            }
        }
    }

    private fun startServer() {
        server = embeddedServer(CIO, port = PORT) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            routing {
                post("/set") {
                    try {
                        val req = call.receive<SetAlarmRequest>()
                        val id = UUID.randomUUID().toString()

                        val now = Calendar.getInstance()
                        val trigger = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, req.hour)
                            set(Calendar.MINUTE, req.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
                        }

                        val entity = AlarmEntity(
                            id = id,
                            hour = req.hour,
                            minute = req.minute,
                            label = req.label,
                            triggerTimeMillis = trigger.timeInMillis
                        )

                        alarmDao.insert(entity)
                        alarmScheduler.schedule(entity)

                        call.respond(HttpStatusCode.Created, SetAlarmResponse(id))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(e.message ?: "Invalid request")
                        )
                    }
                }

                post("/rm") {
                    try {
                        val req = call.receive<RemoveAlarmRequest>()
                        val alarm = alarmDao.getById(req.id)
                        if (alarm != null) {
                            alarmScheduler.cancel(alarm)
                            alarmDao.deleteById(req.id)
                            call.respond(HttpStatusCode.OK, StatusResponse("removed"))
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse("Alarm not found")
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(e.message ?: "Invalid request")
                        )
                    }
                }

                get("/list") {
                    val alarms = alarmDao.getAll()
                    call.respond(alarms)
                }
            }
        }.also { it.start(wait = false) }
    }

    private fun buildNotification(): Notification {
        val ip = getLocalIpAddress() ?: "No network"
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, HeliosApp.CHANNEL_SERVICE)
            .setContentTitle("Helios Server Running")
            .setContentText("Listening on $ip:$PORT")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        const val PORT = 8080
        const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, KtorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KtorService::class.java))
        }
    }
}

@Serializable
data class SetAlarmRequest(val hour: Int, val minute: Int, val label: String = "")

@Serializable
data class RemoveAlarmRequest(val id: String)

@Serializable
data class SetAlarmResponse(val id: String)

@Serializable
data class StatusResponse(val status: String)

@Serializable
data class ErrorResponse(val error: String)
