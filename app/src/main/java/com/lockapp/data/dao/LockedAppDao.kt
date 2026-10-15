package com.lockapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockapp.data.entity.LockedApp
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho bảng locked_apps.
 * Quản lý danh sách ứng dụng bị khóa và trạng thái của chúng.
 */
@Dao
interface LockedAppDao {

    /** Lấy toàn bộ app bị khóa (reactive với Flow) */
    @Query("SELECT * FROM locked_apps ORDER BY category, appName")
    fun getAllLockedApps(): Flow<List<LockedApp>>

    /** Lấy danh sách app đang bật khóa */
    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun getActiveLockedApps(): Flow<List<LockedApp>>

    /** Lấy danh sách app đang bật khóa (không reactive, dùng trong Service) */
    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getActiveLockedAppsList(): List<LockedApp>

    /** Kiểm tra một app có đang bị khóa không */
    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName AND isLocked = 1)")
    suspend fun isAppLocked(packageName: String): Boolean

    /** Lấy app theo packageName */
    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): LockedApp?

    /** Lấy app theo category */
    @Query("SELECT * FROM locked_apps WHERE category = :category")
    fun getByCategory(category: String): Flow<List<LockedApp>>

    /** Lấy danh sách app mặc định của Repair Mode */
    @Query("SELECT * FROM locked_apps WHERE isRepairModeDefault = 1")
    suspend fun getRepairModeDefaults(): List<LockedApp>

    /** Đếm số app đang bị khóa */
    @Query("SELECT COUNT(*) FROM locked_apps WHERE isLocked = 1")
    fun getLockedCount(): Flow<Int>

    /** Thêm app vào danh sách */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedApp)

    /** Thêm nhiều app */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<LockedApp>)

    /** Cập nhật trạng thái app */
    @Update
    suspend fun update(app: LockedApp)

    /** Bật khóa cho 1 app */
    @Query("UPDATE locked_apps SET isLocked = 1 WHERE packageName = :packageName")
    suspend fun lockApp(packageName: String)

    /** Tắt khóa cho 1 app */
    @Query("UPDATE locked_apps SET isLocked = 0 WHERE packageName = :packageName")
    suspend fun unlockApp(packageName: String)

    /** Bật khóa tất cả app có isRepairModeDefault = true */
    @Query("UPDATE locked_apps SET isLocked = 1 WHERE isRepairModeDefault = 1")
    suspend fun lockAllRepairModeDefaults()

    /** Tắt khóa tất cả */
    @Query("UPDATE locked_apps SET isLocked = 0")
    suspend fun unlockAll()

    /** Xóa app khỏi danh sách */
    @Delete
    suspend fun delete(app: LockedApp)

    /** Xóa toàn bộ */
    @Query("DELETE FROM locked_apps")
    suspend fun deleteAll()
}
