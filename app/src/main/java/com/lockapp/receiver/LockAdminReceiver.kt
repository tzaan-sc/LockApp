package com.lockapp.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Device Administrator Receiver - Chống gỡ cài đặt LockApp.
 * Khi được kích hoạt, người dùng không thể gỡ app mà không
 * tắt quyền admin trước (cần biết mật khẩu).
 */
class LockAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "LockAdmin"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        Toast.makeText(context, "Chống gỡ cài đặt đã được kích hoạt", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device admin disabled")
        Toast.makeText(context, "Chống gỡ cài đặt đã bị tắt", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "Device admin disable requested!")
        return "Tắt quyền quản trị sẽ cho phép gỡ cài đặt LockApp. Bạn có chắc chắn?"
    }
}
