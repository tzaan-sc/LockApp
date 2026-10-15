package com.lockapp.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lockapp.camera.IntruderCaptureManager
import com.lockapp.service.LockOverlayManager

/**
 * Activity trong suốt chuyên dùng để kích hoạt BiometricPrompt API (mở khóa bằng vân tay).
 * Do BiometricPrompt yêu cầu FragmentActivity nên Activity này được gọi khi bấm nút vân tay
 * trên màn hình khóa overlay.
 */
class BiometricPromptActivity : FragmentActivity() {

    companion object {
        private const val TAG = "BiometricAuth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showBiometricDialog()
    }

    private fun showBiometricDialog() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric authentication succeeded")
                    LockOverlayManager.getInstance(this@BiometricPromptActivity).onUnlockSuccess()
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d(TAG, "Biometric auth error [$errorCode]: $errString")
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric auth failed (fingerprint unrecognised)")
                    val overlay = LockOverlayManager.getInstance(this@BiometricPromptActivity)
                    val pkg = overlay.getCurrentLockedPackage()
                    if (pkg != null) {
                        IntruderCaptureManager.getInstance(this@BiometricPromptActivity)
                            .captureIntruder(pkg, wrongAttempts = 1, note = "Vân tay không hợp lệ")
                        IntruderCaptureManager.getInstance(this@BiometricPromptActivity).vibrateOnError()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực vân tay")
            .setSubtitle("Quét vân tay để mở khóa ứng dụng")
            .setNegativeButtonText("Dùng mã PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun finish() {
        super.finish()
        // Tắt animation chuyển đổi cửa sổ để liền mạch
        overridePendingTransition(0, 0)
    }
}
