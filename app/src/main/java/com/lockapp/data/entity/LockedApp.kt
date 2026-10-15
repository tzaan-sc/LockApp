package com.lockapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một ứng dụng bị khóa.
 * Lưu thông tin package, tên hiển thị, trạng thái khóa và phân loại.
 */
@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey
    val packageName: String,

    /** Tên hiển thị của app (VD: "Zalo", "Messenger") */
    val appName: String,

    /** Đường dẫn icon app (URI string) */
    val iconUri: String? = null,

    /** Trạng thái khóa */
    val isLocked: Boolean = false,

    /**
     * Phân loại app:
     * - social: Mạng xã hội (Zalo, Facebook, Instagram...)
     * - messaging: Tin nhắn (SMS, Messenger, Telegram...)
     * - media: Ảnh/Video (Gallery, Google Photos...)
     * - finance: Tài chính (Banking apps, Ví điện tử...)
     * - system: Hệ thống (Settings, Play Store...)
     * - other: Khác
     */
    val category: String = "other",

    /** App này có nằm trong danh sách mặc định Repair Mode không */
    val isRepairModeDefault: Boolean = false,

    /** Thời gian thêm vào danh sách */
    val addedTimestamp: Long = System.currentTimeMillis()
)
