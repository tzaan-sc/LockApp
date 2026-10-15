package com.lockapp.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service - Lắng nghe sự kiện TYPE_WINDOW_STATE_CHANGED
 * để phát hiện khi người dùng mở một app bị khóa.
 *
 * Đây là phương pháp nhanh nhất (<15ms) để phát hiện app được mở,
 * vượt trội hơn UsageStatsManager (polling chậm hơn).
 */
class LockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LockAccessibility"

        // Biến static để các component khác kiểm tra service có đang chạy không
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                val className = event.className?.toString() ?: ""

                // Bỏ qua chính app LockApp
                if (packageName == "com.lockapp") return

                // Bỏ qua các system UI (keyboard, status bar, etc.)
                if (isSystemUiPackage(packageName)) return

                Log.d(TAG, "Window changed: $packageName / $className")

                // TODO: Giai đoạn 2 sẽ thêm logic:
                // 1. Kiểm tra packageName có trong danh sách app bị khóa không
                // 2. Nếu có → gọi LockOverlayManager.showOverlay()
                // 3. Đồng thời gọi IntruderCaptureManager.capturePhoto()
                handleAppOpened(packageName, className)
            }
        }
    }

    /**
     * Xử lý khi phát hiện một app được mở.
     * Sẽ được mở rộng ở Giai đoạn 2 với overlay và chụp ảnh.
     */
    private fun handleAppOpened(packageName: String, className: String) {
        // Giai đoạn 1: Chỉ log để test
        Log.d(TAG, "App opened detected: $packageName")

        // TODO Giai đoạn 2:
        // val isLocked = repository.isAppLocked(packageName)
        // if (isLocked) {
        //     LockOverlayManager.getInstance().showLockOverlay(packageName)
        //     IntruderCaptureManager.capturePhoto(packageName)
        // }
    }

    /**
     * Lọc bỏ các package hệ thống không cần giám sát
     */
    private fun isSystemUiPackage(packageName: String): Boolean {
        return packageName in listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.sec.android.inputmethod",
            "com.android.packageinstaller",
        )
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Accessibility Service destroyed")
    }
}
