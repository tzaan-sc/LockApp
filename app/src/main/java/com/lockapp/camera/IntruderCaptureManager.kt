package com.lockapp.camera

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.lockapp.data.AppDatabase
import com.lockapp.data.entity.IntruderLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Quản lý chụp ảnh ngầm camera trước khi có người cố gắng mở app bị khóa
 * hoặc nhập sai mật khẩu PIN.
 *
 * Sử dụng CameraX mà không cần hiển thị Preview, không đèn flash, không âm thanh.
 * Ảnh được lưu trong thư mục internal storage của app (không hiện trong Gallery).
 */
class IntruderCaptureManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IntruderCapture"

        @Volatile
        private var instance: IntruderCaptureManager? = null

        fun getInstance(context: Context): IntruderCaptureManager {
            return instance ?: synchronized(this) {
                instance ?: IntruderCaptureManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.getInstance(context)
    private var isCapturing = false

    /**
     * LifecycleOwner tùy chỉnh để CameraX có thể bind trong Service/Background.
     */
    private class BackgroundLifecycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    /**
     * Chụp ảnh kẻ xâm nhập ngầm và ghi nhật ký vào Room Database.
     *
     * @param targetAppPackage Package name của ứng dụng bị mở
     * @param targetAppName Tên hiển thị của ứng dụng
     * @param wrongAttempts Số lần nhập sai PIN (0 nếu chụp ngay khi vừa chạm mở app)
     * @param note Ghi chú bổ sung nếu có
     */
    fun captureIntruder(
        targetAppPackage: String,
        targetAppName: String? = null,
        wrongAttempts: Int = 0,
        note: String? = null
    ) {
        // Kiểm tra quyền camera
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted, recording log without photo")
            saveLogToDb(
                photoPath = null,
                targetAppPackage = targetAppPackage,
                targetAppName = targetAppName ?: targetAppPackage,
                wrongAttempts = wrongAttempts,
                note = "Quyền camera chưa được cấp"
            )
            return
        }

        val repairModeManager = com.lockapp.util.RepairModeManager(context)
        if (!repairModeManager.isCameraCaptureEnabled()) {
            Log.d(TAG, "Intruder camera is disabled in settings, skipping photo")
            saveLogToDb(
                photoPath = null,
                targetAppPackage = targetAppPackage,
                targetAppName = targetAppName ?: targetAppPackage,
                wrongAttempts = wrongAttempts,
                note = note ?: "Chụp ảnh đã bị tắt trong cài đặt"
            )
            return
        }

        // Tránh chụp đồng thời nhiều ảnh gây xung đột camera
        if (isCapturing) {
            Log.d(TAG, "Capture already in progress, skipping")
            return
        }

        isCapturing = true

        val appDisplayName = targetAppName ?: getAppNameFromPackage(targetAppPackage)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Chọn camera trước, nếu không có thì fallback sang camera sau
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    Log.e(TAG, "No camera available on this device")
                    saveLogToDb(null, targetAppPackage, appDisplayName, wrongAttempts, "Thiết bị không có camera")
                    isCapturing = false
                    return@addListener
                }

                // Cấu hình ImageCapture: không flash, độ trễ thấp nhất
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .build()

                val lifecycleOwner = BackgroundLifecycleOwner()

                // Unbind trước để dọn dẹp
                cameraProvider.unbindAll()

                // Bind camera với lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                // Chuẩn bị file lưu ảnh
                val storageDir = File(context.filesDir, "intruders").apply {
                    if (!exists()) mkdirs()
                }
                val photoFile = File(
                    storageDir,
                    "intruder_${System.currentTimeMillis()}_${targetAppPackage.replace(".", "_")}.jpg"
                )

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Intruder photo saved: ${photoFile.absolutePath}")
                            cameraProvider.unbindAll()
                            lifecycleOwner.destroy()
                            isCapturing = false

                            saveLogToDb(
                                photoPath = photoFile.absolutePath,
                                targetAppPackage = targetAppPackage,
                                targetAppName = appDisplayName,
                                wrongAttempts = wrongAttempts,
                                note = note
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Image capture error: ${exception.message}", exception)
                            cameraProvider.unbindAll()
                            lifecycleOwner.destroy()
                            isCapturing = false

                            saveLogToDb(
                                photoPath = null,
                                targetAppPackage = targetAppPackage,
                                targetAppName = appDisplayName,
                                wrongAttempts = wrongAttempts,
                                note = "Lỗi chụp: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera: ${e.message}", e)
                isCapturing = false
                saveLogToDb(
                    photoPath = null,
                    targetAppPackage = targetAppPackage,
                    targetAppName = appDisplayName,
                    wrongAttempts = wrongAttempts,
                    note = "Lỗi khởi tạo camera: ${e.message}"
                )
            }
        }, mainExecutor)
    }

    /**
     * Rung phản hồi khi nhập sai PIN.
     */
    fun vibrateOnError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 80, 150),
                    intArrayOf(0, 200, 0, 255),
                    -1
                )
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 80, 150),
                        -1
                    )
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 100, 80, 150), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }
    }

    private fun saveLogToDb(
        photoPath: String?,
        targetAppPackage: String,
        targetAppName: String,
        wrongAttempts: Int,
        note: String?
    ) {
        scope.launch {
            try {
                val log = IntruderLog(
                    photoPath = photoPath,
                    timestamp = System.currentTimeMillis(),
                    targetAppPackage = targetAppPackage,
                    targetAppName = targetAppName,
                    wrongAttempts = wrongAttempts,
                    note = note
                )
                val id = database.intruderLogDao().insert(log)
                Log.d(TAG, "Intruder log recorded with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save intruder log: ${e.message}", e)
            }
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
