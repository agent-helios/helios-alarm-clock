package com.example.helios_alarm_clock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helios_alarm_clock.data.AlarmDao
import com.example.helios_alarm_clock.data.AlarmEntity
import com.example.helios_alarm_clock.service.KtorService
import com.example.helios_alarm_clock.util.AlarmScheduler
import com.example.helios_alarm_clock.util.getLocalIpAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ipAddress: String
        get() = getLocalIpAddress() ?: "No network"

    val port: Int = KtorService.PORT

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
