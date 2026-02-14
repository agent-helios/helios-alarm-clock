package com.example.helios_alarm_clock.di

import android.content.Context
import androidx.room.Room
import com.example.helios_alarm_clock.data.AlarmDao
import com.example.helios_alarm_clock.data.AlarmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase =
        Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.NAME
        ).build()

    @Provides
    @Singleton
    fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()
}
