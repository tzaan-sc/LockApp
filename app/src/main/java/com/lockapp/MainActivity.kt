package com.lockapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lockapp.camera.IntruderCaptureManager
import com.lockapp.ui.auth.BiometricAuthHelper
import com.lockapp.ui.navigation.LockAppNavHost
import com.lockapp.ui.screens.LockOverlayScreen
import com.lockapp.ui.theme.LockAppTheme
import com.lockapp.util.RepairModeManager

class MainActivity : FragmentActivity() {

    private lateinit var repairModeManager: RepairModeManager
    private var isAppAuthenticated by mutableStateOf(false)
    private var hasPromptedBiometric = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repairModeManager = RepairModeManager(this)

        // Nếu chưa hoàn tất thiết lập ban đầu (chưa tạo PIN), cho phép vào thẳng SetupScreen
        if (!repairModeManager.isSetupComplete()) {
            isAppAuthenticated = true
        }

        enableEdgeToEdge()
        setContent {
            LockAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (repairModeManager.isSetupComplete() && !isAppAuthenticated) {
                        LockOverlayScreen(
                            appName = getString(R.string.app_name),
                            appPackage = packageName,
                            appIcon = ContextCompat.getDrawable(this, R.mipmap.ic_launcher),
                            isBiometricAvailable = BiometricAuthHelper.isBiometricAvailable(this) && repairModeManager.isBiometricEnabled(),
                            onVerifyPin = { pin ->
                                repairModeManager.verifyPin(pin)
                            },
                            onUnlockSuccess = {
                                isAppAuthenticated = true
                            },
                            onWrongPin = { attemptCount ->
                                IntruderCaptureManager.getInstance(this).vibrateOnError()
                                IntruderCaptureManager.getInstance(this).captureIntruder(
                                    targetAppPackage = packageName,
                                    targetAppName = getString(R.string.app_name),
                                    wrongAttempts = attemptCount,
                                    note = "Cố gắng mở LockApp sai PIN lần $attemptCount"
                                )
                            },
                            onBiometricClick = {
                                showBiometricPrompt()
                            },
                            onExitToHome = {
                                finish()
                            }
                        )
                    } else {
                        LockAppNavHost()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Khi mở app, nếu đã thiết lập và có bật vân tay, tự động gợi ý quét vân tay (chỉ 1 lần mỗi phiên mở)
        if (repairModeManager.isSetupComplete() && !isAppAuthenticated && !hasPromptedBiometric) {
            if (BiometricAuthHelper.isBiometricAvailable(this) && repairModeManager.isBiometricEnabled()) {
                hasPromptedBiometric = true
                showBiometricPrompt()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Khi người dùng thoát ra khỏi app hoặc tắt màn hình, yêu cầu xác thực lại khi quay lại
        if (repairModeManager.isSetupComplete()) {
            isAppAuthenticated = false
            hasPromptedBiometric = false
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAppAuthenticated = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    IntruderCaptureManager.getInstance(this@MainActivity)
                        .captureIntruder(
                            targetAppPackage = packageName,
                            targetAppName = getString(R.string.app_name),
                            wrongAttempts = 1,
                            note = "Mở LockApp sai vân tay"
                        )
                    IntruderCaptureManager.getInstance(this@MainActivity).vibrateOnError()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực LockApp")
            .setSubtitle("Quét vân tay để truy cập ứng dụng")
            .setNegativeButtonText("Dùng mã PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
