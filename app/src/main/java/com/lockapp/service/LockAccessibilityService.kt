package com.lockapp.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lockapp.camera.IntruderCaptureManager
import com.lockapp.data.AppDatabase
import com.lockapp.util.RepairModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accessibility Service - Lắng nghe sự kiện TYPE_WINDOW_STATE_CHANGED
 * để phát hiện khi người dùng mở một app bị khóa (< 15ms).
 *
 * Kết nối luồng:
 * Accessibility Event -> Kiểm tra khóa -> LockOverlayManager (Hiển thị PIN) -> IntruderCaptureManager (Chụp ảnh ngầm)
 */
class LockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LockAccessibility"

        var isRunning = false
            private set
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var overlayManager: LockOverlayManager
    private lateinit var captureManager: IntruderCaptureManager
    private lateinit var repairModeManager: RepairModeManager

    // Ghi nhớ app vừa được người dùng mở khóa hợp lệ trong phiên này
    private var tempUnlockedPackage: String? = null
    private var lastPackageName: String? = null

    // Danh sách package mặc định của Repair Mode (Chế độ sửa máy)
    private val repairModeDefaultPackages = setOf(
        "com.android.settings",
        "com.zing.zalo",
        "com.facebook.orca",
        "com.facebook.katana",
        "com.google.android.apps.photos",
        "com.sec.android.gallery3d",
        "com.android.gallery3d",
        "com.android.vending",
        "com.google.android.gm",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        database = AppDatabase.getInstance(this)
        overlayManager = LockOverlayManager.getInstance(this)
        captureManager = IntruderCaptureManager.getInstance(this)
        repairModeManager = RepairModeManager(this)

        // Lắng nghe callback mở khóa thành công để cấp phiên tạm thời
        overlayManager.onUnlockListener = { packageName ->
            Log.d(TAG, "Package temporarily unlocked: $packageName")
            tempUnlockedPackage = packageName
        }

        Log.d(TAG, "LockAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                // Bỏ qua chính app LockApp
                if (packageName == applicationContext.packageName) return

                // Bỏ qua các system UI (bàn phím, status bar...)
                if (isSystemUiPackage(packageName)) return

                // Nếu chuyển sang một app khác không phải app đã mở khóa, reset phiên tạm
                if (tempUnlockedPackage != null && packageName != tempUnlockedPackage && !isLauncherPackage(packageName)) {
                    Log.d(TAG, "Navigated away from unlocked app $tempUnlockedPackage to $packageName, resetting session")
                    tempUnlockedPackage = null
                }

                // Nếu là app đang được cấp phép trong phiên tạm, cho phép truy cập
                if (packageName == tempUnlockedPackage) {
                    lastPackageName = packageName
                    return
                }

                if (packageName != lastPackageName || !overlayManager.isShowing()) {
                    lastPackageName = packageName
                    checkAndLockApp(packageName)
                }
            }
        }
    }

    /**
     * Kiểm tra ứng dụng có thuộc diện bị khóa hay không
     */
    private fun checkAndLockApp(packageName: String) {
        scope.launch {
            try {
                val isRepairMode = repairModeManager.isRepairModeActive()
                val isRepairDefault = isRepairMode && repairModeDefaultPackages.contains(packageName)
                val isDbLocked = database.lockedAppDao().isAppLocked(packageName)

                if (isRepairDefault || isDbLocked) {
                    Log.d(TAG, "App $packageName is locked! Showing overlay and taking silent photo")

                    // 1. Hiển thị màn hình khóa Overlay
                    overlayManager.showLockOverlay(packageName)

                    // 2. Tự động chụp ảnh ngầm camera trước
                    captureManager.captureIntruder(
                        targetAppPackage = packageName,
                        wrongAttempts = 0,
                        note = "Mở app khi đang bảo vệ"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking app lock status: ${e.message}", e)
            }
        }
    }

    private fun isSystemUiPackage(packageName: String): Boolean {
        return packageName in listOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.sec.android.inputmethod",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )
    }

    private fun isLauncherPackage(packageName: String): Boolean {
        return packageName in listOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher"
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
