package com.example.helios_alarm_clock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val triggerTimeMillis: Long
)
