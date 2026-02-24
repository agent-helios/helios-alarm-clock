package com.example.helios_alarm_clock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class], version = 3, exportSchema = true)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val NAME = "helios_alarms.db"

        // Fresh install path: add date column with correct default
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN date TEXT DEFAULT ''")
            }
        }

        // Fix path: first install had DEFAULT NULL instead of DEFAULT ''
        // SQLite can't alter column defaults, so recreate the table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE alarms_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        triggerTimeMillis INTEGER NOT NULL,
                        date TEXT DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO alarms_new (id, hour, minute, label, triggerTimeMillis, date)
                    SELECT id, hour, minute, label, triggerTimeMillis, COALESCE(date, '')
                    FROM alarms
                """.trimIndent())
                db.execSQL("DROP TABLE alarms")
                db.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }
    }
}
