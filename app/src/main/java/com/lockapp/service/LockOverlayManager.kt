package com.lockapp.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

/**
 * Quản lý hiển thị Overlay màn hình khóa.
 * Sử dụng WindowManager để vẽ view đè lên toàn bộ ứng dụng khác.
 *
 * Giai đoạn 1: Khởi tạo cấu trúc cơ bản.
 * Giai đoạn 2: Sẽ thêm giao diện nhập PIN bằng Compose.
 */
class LockOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "LockOverlay"

        @Volatile
        private var instance: LockOverlayManager? = null

        fun getInstance(context: Context): LockOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: LockOverlayManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var isOverlayShowing = false
    private var currentLockedPackage: String? = null

    /**
     * Tạo LayoutParams cho overlay window.
     * TYPE_APPLICATION_OVERLAY cho phép hiển thị trên mọi app khác.
     */
    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    /**
     * Hiển thị overlay khóa cho một app cụ thể.
     * TODO: Giai đoạn 2 - Thêm Compose View với giao diện nhập PIN.
     */
    fun showLockOverlay(packageName: String) {
        if (isOverlayShowing && currentLockedPackage == packageName) {
            Log.d(TAG, "Overlay already showing for $packageName")
            return
        }

        Log.d(TAG, "Showing lock overlay for: $packageName")
        currentLockedPackage = packageName
        isOverlayShowing = true

        // TODO Giai đoạn 2: Tạo ComposeView với giao diện PIN và add vào WindowManager
    }

    /**
     * Ẩn overlay khi nhập đúng PIN.
     */
    fun hideLockOverlay() {
        if (!isOverlayShowing) return

        Log.d(TAG, "Hiding lock overlay for: $currentLockedPackage")
        isOverlayShowing = false
        currentLockedPackage = null

        // TODO Giai đoạn 2: Remove view khỏi WindowManager
    }

    fun isShowing(): Boolean = isOverlayShowing

    fun getCurrentLockedPackage(): String? = currentLockedPackage
}
