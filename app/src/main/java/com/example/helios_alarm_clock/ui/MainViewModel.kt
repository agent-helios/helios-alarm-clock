package com.example.helios_alarm_clock.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helios_alarm_clock.data.AlarmDao
import com.example.helios_alarm_clock.data.AlarmEntity
import com.example.helios_alarm_clock.service.KtorService
import com.example.helios_alarm_clock.util.AlarmScheduler
import com.example.helios_alarm_clock.util.getLocalIpAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _serverRunning = MutableStateFlow(false)
    val serverRunning: StateFlow<Boolean> = _serverRunning.asStateFlow()

    val ipAddress: String
        get() = getLocalIpAddress() ?: "No network"

    val port: Int = KtorService.PORT

    fun startServer() {
        KtorService.start(context)
        _serverRunning.value = true
    }

    fun stopServer() {
        KtorService.stop(context)
        _serverRunning.value = false
    }

    fun createAlarm(hour: Int, minute: Int, label: String) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val trigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val entity = AlarmEntity(
                id = UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                label = label,
                triggerTimeMillis = trigger.timeInMillis
            )
            alarmDao.insert(entity)
            alarmScheduler.schedule(entity)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(alarm)
            alarmDao.deleteById(alarm.id)
        }
    }
}
