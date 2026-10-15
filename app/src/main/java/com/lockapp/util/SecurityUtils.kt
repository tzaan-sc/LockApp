package com.lockapp.util

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Tiện ích bảo mật: hash mật khẩu bằng PBKDF2.
 * Đảm bảo mật khẩu được lưu trữ an toàn, không thể reverse.
 */
object SecurityUtils {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    /**
     * Tạo hash từ mật khẩu gốc.
     * @return Pair(hash, salt) dưới dạng hex string
     */
    fun hashPassword(password: String): Pair<String, String> {
        val salt = generateSalt()
        val hash = pbkdf2Hash(password, salt)
        return Pair(hash.toHexString(), salt.toHexString())
    }

    /**
     * Xác thực mật khẩu với hash đã lưu.
     */
    fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean {
        val salt = storedSalt.hexToByteArray()
        val computedHash = pbkdf2Hash(password, salt)
        return computedHash.toHexString() == storedHash
    }

    private fun pbkdf2Hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
