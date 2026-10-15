# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.lockapp.data.entity.** { *; }

# Keep Device Admin Receiver
-keep class com.lockapp.receiver.LockAdminReceiver { *; }

# Keep Accessibility Service
-keep class com.lockapp.service.LockAccessibilityService { *; }
