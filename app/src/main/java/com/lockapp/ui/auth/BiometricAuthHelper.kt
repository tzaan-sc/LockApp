package com.lockapp.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager

/**
 * Tiện ích kiểm tra khả năng hỗ trợ và trạng thái sinh trắc học (vân tay) của thiết bị.
 */
object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }
}
