# Add project specific ProGuard rules here.

# Keep Room Database & Entities
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**
-keep class com.lockapp.data.entity.** { *; }
-keep class com.lockapp.data.dao.** { *; }

# Keep Services, Receivers & Application
-keep class com.lockapp.LockApplication { *; }
-keep class com.lockapp.receiver.LockAdminReceiver { *; }
-keep class com.lockapp.receiver.BootReceiver { *; }
-keep class com.lockapp.service.LockAccessibilityService { *; }
-keep class com.lockapp.service.LockService { *; }

# Keep CameraX
-keep class androidx.camera.core.** { *; }
-dontwarn androidx.camera.core.**

# Keep Biometric & Security Crypto
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }

# Suppress warnings for compile-only errorprone annotations (Tink Crypto)
-dontwarn com.google.errorprone.annotations.**

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Coil
-keep class coil.** { *; }
