package com.lockapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity ghi lại mỗi lần xâm nhập (ai đó cố mở app bị khóa).
 * Lưu ảnh chụp mặt, thời gian, app nào bị mở, số lần sai PIN.
 */
@Entity(tableName = "intruder_logs")
data class IntruderLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Đường dẫn file ảnh chụp mặt người xâm nhập */
    val photoPath: String? = null,

    /** Thời gian phát hiện xâm nhập (timestamp milliseconds) */
    val timestamp: Long = System.currentTimeMillis(),

    /** Package name của app bị cố gắng mở */
    val targetAppPackage: String,

    /** Tên hiển thị của app bị cố gắng mở */
    val targetAppName: String,

    /** Số lần nhập sai mật khẩu */
    val wrongAttempts: Int = 0,

    /** Ghi chú thêm (nếu có) */
    val note: String? = null
)
