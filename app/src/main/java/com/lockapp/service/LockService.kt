package com.lockapp.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lockapp.LockApplication
import com.lockapp.MainActivity
import com.lockapp.R
import com.lockapp.data.AppDatabase
import com.lockapp.util.RepairModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service chạy ngầm liên tục để giám sát các ứng dụng.
 * Hiển thị persistent notification với số lượng app bảo vệ thời gian thực.
 */
class LockService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.lockapp.action.START"
        const val ACTION_STOP = "com.lockapp.action.STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null
    private var currentLockedCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startObservingLockedCount()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                observeJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification(currentLockedCount))
                if (observeJob == null || observeJob?.isActive == false) {
                    startObservingLockedCount()
                }
            }
        }
        return START_STICKY
    }

    private fun startObservingLockedCount() {
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            try {
                val database = AppDatabase.getInstance(this@LockService)
                database.lockedAppDao().getLockedCount().collect { count ->
                    currentLockedCount = count
                    updateNotification(count)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun updateNotification(count: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, createNotification(count))
    }

    private fun createNotification(count: Int = 0): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val repairModeManager = RepairModeManager(this)
        val isRepair = repairModeManager.isRepairModeActive()

        val contentText = if (isRepair) {
            getString(R.string.notification_text_repair_mode, count)
        } else {
            getString(R.string.notification_text, count)
        }

        return NotificationCompat.Builder(this, LockApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
