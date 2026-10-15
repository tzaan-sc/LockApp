package com.lockapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lockapp.service.LockService

/**
 * Boot Receiver - Tự động khởi động LockService khi máy reboot.
 * Đảm bảo bảo vệ app luôn hoạt động ngay cả sau khi khởi động lại thiết bị.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed - Starting LockService")

            val serviceIntent = Intent(context, LockService::class.java).apply {
                action = LockService.ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
