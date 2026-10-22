package com.lockapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Quản lý logic Repair Mode (Chế độ sửa máy).
 * Lưu trạng thái bật/tắt và điều phối khóa/mở khóa hàng loạt app.
 */
class RepairModeManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "repair_mode_prefs"
        private const val KEY_REPAIR_MODE_ACTIVE = "repair_mode_active"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_CAMERA_CAPTURE_ENABLED = "camera_capture_enabled"
        private const val KEY_SCRAMBLE_PINPAD_ENABLED = "scramble_pinpad_enabled"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback nếu EncryptedSharedPreferences bị lỗi
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /** Kiểm tra Repair Mode có đang bật không */
    fun isRepairModeActive(): Boolean {
        return prefs.getBoolean(KEY_REPAIR_MODE_ACTIVE, false)
    }

    /** Bật Repair Mode */
    fun activateRepairMode() {
        prefs.edit().putBoolean(KEY_REPAIR_MODE_ACTIVE, true).apply()
    }

    /** Tắt Repair Mode */
    fun deactivateRepairMode() {
        prefs.edit().putBoolean(KEY_REPAIR_MODE_ACTIVE, false).apply()
    }

    /** Lưu PIN hash */
    fun savePinHash(hash: String, salt: String) {
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    /** Xác thực PIN (mặc định 1234 nếu chưa thiết lập) */
    fun verifyPin(pin: String): Boolean {
        if (!isPinSet()) {
            return pin == "1234"
        }
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return SecurityUtils.verifyPassword(pin, storedHash, storedSalt)
    }

    /** Kiểm tra đã thiết lập PIN chưa */
    fun isPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    /** Đánh dấu đã hoàn tất thiết lập */
    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    /** Kiểm tra đã hoàn tất thiết lập chưa */
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    /** Bật/tắt mở khóa bằng vân tay */
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /** Kiểm tra vân tay có được bật không */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /** Bật/tắt tự động chụp ảnh ngầm */
    fun setCameraCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAMERA_CAPTURE_ENABLED, enabled).apply()
    }

    /** Kiểm tra chụp ảnh ngầm có được bật không (mặc định bật) */
    fun isCameraCaptureEnabled(): Boolean {
        return prefs.getBoolean(KEY_CAMERA_CAPTURE_ENABLED, true)
    }

    /** Bật/tắt xáo trộn vị trí bàn phím số PIN */
    fun setScramblePinpadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCRAMBLE_PINPAD_ENABLED, enabled).apply()
    }

    /** Kiểm tra xáo trộn bàn phím số có được bật không (mặc định tắt) */
    fun isScramblePinpadEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCRAMBLE_PINPAD_ENABLED, false)
    }

    /** Đổi mã PIN bảo mật */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        val (newHash, newSalt) = SecurityUtils.hashPassword(newPin)
        savePinHash(newHash, newSalt)
        return true
    }
}
