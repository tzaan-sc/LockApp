package com.lockapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lockapp.data.dao.IntruderLogDao
import com.lockapp.data.dao.LockedAppDao
import com.lockapp.data.entity.IntruderLog
import com.lockapp.data.entity.LockedApp

/**
 * Room Database chính của LockApp.
 * Quản lý 2 bảng: locked_apps (danh sách app khóa) và intruder_logs (nhật ký xâm nhập).
 */
@Database(
    entities = [LockedApp::class, IntruderLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lockedAppDao(): LockedAppDao
    abstract fun intruderLogDao(): IntruderLogDao

    companion object {
        private const val DATABASE_NAME = "lockapp_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
