package com.lockapp.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.lockapp.camera.IntruderCaptureManager
import com.lockapp.ui.auth.BiometricAuthHelper
import com.lockapp.ui.auth.BiometricPromptActivity
import com.lockapp.ui.screens.LockOverlayScreen
import com.lockapp.util.RepairModeManager

/**
 * Quản lý hiển thị Overlay màn hình khóa bằng Jetpack Compose đè lên ứng dụng bị khóa.
 * Chạy trực tiếp qua WindowManager trong background service mà không làm mất trạng thái của app.
 */
class LockOverlayManager private constructor(private val context: Context) {

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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val repairModeManager = RepairModeManager(context)

    private var overlayView: View? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var isOverlayShowing = false
    private var currentLockedPackage: String? = null

    /** Callback thông báo khi mở khóa thành công */
    var onUnlockListener: ((String) -> Unit)? = null

    /**
     * Hiển thị overlay khóa cho một app cụ thể.
     */
    fun showLockOverlay(packageName: String) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        if (isOverlayShowing && currentLockedPackage == packageName) {
            Log.d(TAG, "Overlay already showing for $packageName")
            return
        }

        mainHandler.post {
            try {
                // Nếu đang hiển thị overlay cũ thì xóa trước
                if (overlayView != null) {
                    removeOverlayViewInternal()
                }

                currentLockedPackage = packageName
                isOverlayShowing = true
                Log.d(TAG, "Showing lock overlay for: $packageName")

                val pm = context.packageManager
                val (appName, appIcon) = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    Pair(pm.getApplicationLabel(appInfo).toString(), pm.getApplicationIcon(appInfo))
                } catch (e: Exception) {
                    Pair(packageName, null)
                }

                val exitToHomeAction: () -> Unit = {
                    hideLockOverlay()
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                }

                val lifecycleOwner = OverlayLifecycleOwner(onBackPressed = exitToHomeAction)
                overlayLifecycleOwner = lifecycleOwner

                val rootLayout = OverlayRootLayout(context, onBackPress = exitToHomeAction)
                lifecycleOwner.attachToView(rootLayout)

                val composeView = ComposeView(context).apply {
                    lifecycleOwner.attachToView(this)
                    setContent {
                        LockOverlayScreen(
                            appName = appName,
                            appPackage = packageName,
                            appIcon = appIcon,
                            isBiometricAvailable = BiometricAuthHelper.isBiometricAvailable(context),
                            isScramblePinpadEnabled = repairModeManager.isScramblePinpadEnabled(),
                            onVerifyPin = { pin ->
                                repairModeManager.verifyPin(pin)
                            },
                            onUnlockSuccess = {
                                onUnlockSuccess()
                            },
                            onWrongPin = { attemptCount ->
                                IntruderCaptureManager.getInstance(context).vibrateOnError()
                                val threshold = repairModeManager.getCaptureThreshold()
                                if (threshold == 0 || attemptCount >= threshold) {
                                    IntruderCaptureManager.getInstance(context).captureIntruder(
                                        targetAppPackage = packageName,
                                        targetAppName = appName,
                                        wrongAttempts = attemptCount,
                                        note = "Nhập sai PIN lần thứ $attemptCount"
                                    )
                                }
                            },
                            onBiometricClick = {
                                val intent = Intent(context, BiometricPromptActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                }
                                context.startActivity(intent)
                            },
                            onExitToHome = exitToHomeAction
                        )
                    }
                }
                rootLayout.addView(composeView)

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

                windowManager.addView(rootLayout, layoutParams)
                rootLayout.requestFocus()
                lifecycleOwner.onResume()
                overlayView = rootLayout

            } catch (e: Exception) {
                Log.e(TAG, "Error displaying lock overlay: ${e.message}", e)
                isOverlayShowing = false
                currentLockedPackage = null
            }
        }
    }

    /**
     * Xử lý khi mở khóa thành công (nhập đúng PIN hoặc vân tay).
     */
    fun onUnlockSuccess() {
        val unlockedPkg = currentLockedPackage
        hideLockOverlay()
        if (unlockedPkg != null) {
            onUnlockListener?.invoke(unlockedPkg)
        }
    }

    /**
     * Ẩn overlay.
     */
    fun hideLockOverlay() {
        mainHandler.post {
            removeOverlayViewInternal()
        }
    }

    private fun removeOverlayViewInternal() {
        try {
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            }
            overlayLifecycleOwner?.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view: ${e.message}", e)
        } finally {
            overlayView = null
            overlayLifecycleOwner = null
            isOverlayShowing = false
            currentLockedPackage = null
        }
    }

    fun isShowing(): Boolean = isOverlayShowing

    fun getCurrentLockedPackage(): String? = currentLockedPackage
}

/**
 * Root FrameLayout chạy trực tiếp trên WindowManager,
 * có khả năng nhận focus và bắt sự kiện KeyEvent.KEYCODE_BACK (phím Back hoặc cử chỉ vuốt Back)
 * để tự động đưa máy về màn hình chính.
 */
private class OverlayRootLayout(
    context: Context,
    private val onBackPress: () -> Unit
) : FrameLayout(context) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                onBackPress()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
