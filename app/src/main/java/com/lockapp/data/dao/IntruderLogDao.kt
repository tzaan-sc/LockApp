package com.lockapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.lockapp.data.entity.IntruderLog
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho bảng intruder_logs.
 * Quản lý nhật ký xâm nhập (ảnh chụp + thông tin).
 */
@Dao
interface IntruderLogDao {

    /** Lấy tất cả log, mới nhất lên đầu */
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntruderLog>>

    /** Lấy log theo app cụ thể */
    @Query("SELECT * FROM intruder_logs WHERE targetAppPackage = :packageName ORDER BY timestamp DESC")
    fun getLogsByApp(packageName: String): Flow<List<IntruderLog>>

    /** Lấy log theo khoảng thời gian */
    @Query("SELECT * FROM intruder_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsByTimeRange(startTime: Long, endTime: Long): Flow<List<IntruderLog>>

    /** Đếm tổng số lần xâm nhập */
    @Query("SELECT COUNT(*) FROM intruder_logs")
    fun getTotalCount(): Flow<Int>

    /** Đếm số lần xâm nhập hôm nay */
    @Query("SELECT COUNT(*) FROM intruder_logs WHERE timestamp >= :todayStart")
    fun getTodayCount(todayStart: Long): Flow<Int>

    /** Thêm bản ghi xâm nhập mới */
    @Insert
    suspend fun insert(log: IntruderLog): Long

    /** Xóa một bản ghi */
    @Delete
    suspend fun delete(log: IntruderLog)

    /** Xóa toàn bộ nhật ký */
    @Query("DELETE FROM intruder_logs")
    suspend fun deleteAll()

    /** Lấy bản ghi theo ID */
    @Query("SELECT * FROM intruder_logs WHERE id = :id")
    suspend fun getById(id: Long): IntruderLog?
}
