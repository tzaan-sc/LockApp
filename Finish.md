# 📝 Tiến Trình Phát Triển LockApp

> **Cập nhật lần cuối:** 15/10/2026 – 09:09

---

## ✅ Bước 1: Khởi Tạo Project & Cấu Trúc Dịch Vụ Chạy Ngầm (Giai đoạn 1)

**Trạng thái:** ✅ Đã hoàn thành & Build APK thành công (`app-debug.apk`)!

### Đã hoàn thành:

#### 1.1 Khởi tạo Project Android (Kotlin + Jetpack Compose + Material 3)
- [x] `settings.gradle.kts` – Cấu hình plugin repositories (Google, Maven Central)
- [x] `build.gradle.kts` (root) – Khai báo plugins: AGP 8.2.2, Kotlin 1.9.22, KSP 1.9.22
- [x] `gradle.properties` – JVM args, AndroidX, code style
- [x] `gradle/wrapper/gradle-wrapper.properties` – Gradle 8.5
- [x] `gradlew.bat` – Gradle wrapper script

#### 1.2 Cấu hình `app/build.gradle.kts` với toàn bộ dependencies
- [x] **compileSdk:** 34, **minSdk:** 26, **targetSdk:** 34
- [x] **Jetpack Compose** + Material 3 (BOM 2024.01.00)
- [x] **CameraX** 1.3.1 (core, camera2, lifecycle)
- [x] **Room Database** 2.6.1 (runtime, ktx, compiler via KSP)
- [x] **EncryptedSharedPreferences** 1.1.0-alpha06
- [x] **BiometricPrompt** 1.2.0-alpha05
- [x] **Coil** 2.5.0 (compose)
- [x] **Navigation Compose** 2.7.6
- [x] **Coroutines** 1.7.3
- [x] **DataStore Preferences** 1.0.0
- [x] ProGuard rules cho Room entities, Device Admin, Accessibility Service

#### 1.3 Khai báo permissions trong `AndroidManifest.xml`
- [x] `SYSTEM_ALERT_WINDOW` – Vẽ overlay màn hình khóa
- [x] `PACKAGE_USAGE_STATS` – Theo dõi app đang mở
- [x] `CAMERA` – Chụp ảnh ngầm cam trước
- [x] `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` – Chạy dịch vụ ngầm
- [x] `POST_NOTIFICATIONS` – Thông báo (Android 13+)
- [x] `RECEIVE_BOOT_COMPLETED` – Tự khởi động sau reboot
- [x] `QUERY_ALL_PACKAGES` – Quét toàn bộ app đã cài
- [x] `VIBRATE` – Feedback rung khi sai PIN
- [x] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` – Tắt tối ưu pin

#### 1.4 Khai báo Services & Receivers trong Manifest
- [x] `LockService` – Foreground Service (specialUse)
- [x] `LockAccessibilityService` – Accessibility Service với config XML
- [x] `LockAdminReceiver` – Device Administrator receiver
- [x] `BootReceiver` – Tự khởi động sau reboot

#### 1.5 Resource Files
- [x] `res/xml/accessibility_config.xml` – Config Accessibility Service
- [x] `res/xml/device_admin_policies.xml` – Config Device Admin (force-lock, wipe-data)
- [x] `res/values/strings.xml` – Toàn bộ chuỗi tiếng Việt
- [x] `res/values/themes.xml` – Theme cơ bản (transparent status bar)
- [x] `res/values/ic_launcher_background.xml` – Màu nền icon (#6366F1)
- [x] `res/mipmap-anydpi-v26/ic_launcher.xml` – Adaptive icon
- [x] `res/mipmap-anydpi-v26/ic_launcher_foreground.xml` – Icon foreground (shield + lock)

#### 1.6 Kotlin Source Code – Cốt lõi
- [x] `LockApplication.kt` – Application class, tạo notification channel
- [x] `MainActivity.kt` – Activity chính, edge-to-edge, Compose content

#### 1.7 Kotlin – UI Theme (Material 3)
- [x] `ui/theme/Color.kt` – Bảng màu curated: Indigo/Violet primary, Cyan secondary, Dark/Light surfaces
- [x] `ui/theme/Theme.kt` – Dark & Light color schemes, transparent status bar
- [x] `ui/theme/Type.kt` – Typography system đầy đủ (display → label)

#### 1.8 Kotlin – Services
- [x] `service/LockService.kt` – Foreground Service với persistent notification, START_STICKY
- [x] `service/LockAccessibilityService.kt` – Phát hiện app mở qua TYPE_WINDOW_STATE_CHANGED, lọc system UI packages
- [x] `service/LockOverlayManager.kt` – Quản lý WindowManager overlay (TYPE_APPLICATION_OVERLAY)

#### 1.9 Kotlin – Receivers
- [x] `receiver/LockAdminReceiver.kt` – Device Admin, cảnh báo khi bị disable
- [x] `receiver/BootReceiver.kt` – Tự khởi động LockService sau boot

#### 1.10 Kotlin – Data Layer (Room Database)
- [x] `data/entity/LockedApp.kt` – Entity: packageName, appName, isLocked, category, isRepairModeDefault
- [x] `data/entity/IntruderLog.kt` – Entity: id, photoPath, timestamp, targetApp, wrongAttempts
- [x] `data/dao/LockedAppDao.kt` – DAO với Flow queries, batch lock/unlock, repair mode operations
- [x] `data/dao/IntruderLogDao.kt` – DAO với time-based queries, per-app filtering, counts
- [x] `data/AppDatabase.kt` – Room Database singleton (2 tables)

#### 1.11 Kotlin – Utilities
- [x] `util/SecurityUtils.kt` – PBKDF2 password hashing (120k iterations, 256-bit key, random salt)
- [x] `util/PermissionHelper.kt` – Kiểm tra & xin quyền: Overlay, Accessibility, Camera, Usage Stats, Device Admin, USB Debugging
- [x] `util/RepairModeManager.kt` – Quản lý Repair Mode state bằng EncryptedSharedPreferences

#### 1.12 Kotlin – Navigation & Screens
- [x] `ui/navigation/Navigation.kt` – NavHost với 5 routes: Home, AppList, IntruderLog, Settings, Setup
- [x] `ui/screens/HomeScreen.kt` – Màn hình chính:
  - Nút gạt Repair Mode có animation (scale, color)
  - Status cards (số app bảo vệ, số lần xâm nhập)
  - Quick actions: Quản lý app khóa, Nhật ký xâm nhập, Trạng thái quyền
  - USB Debugging warning card

---

### 📊 Cấu trúc thư mục đã tạo:
```
LockApp/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── xml/accessibility_config.xml
│       │   ├── xml/device_admin_policies.xml
│       │   ├── values/strings.xml
│       │   ├── values/themes.xml
│       │   ├── values/ic_launcher_background.xml
│       │   └── mipmap-anydpi-v26/
│       │       ├── ic_launcher.xml
│       │       └── ic_launcher_foreground.xml
│       └── java/com/lockapp/
│           ├── LockApplication.kt
│           ├── MainActivity.kt
│           ├── ui/
│           │   ├── theme/ (Color.kt, Theme.kt, Type.kt)
│           │   ├── navigation/Navigation.kt
│           │   └── screens/HomeScreen.kt
│           ├── service/
│           │   ├── LockService.kt
│           │   ├── LockAccessibilityService.kt
│           │   └── LockOverlayManager.kt
│           ├── receiver/
│           │   ├── LockAdminReceiver.kt
│           │   └── BootReceiver.kt
│           ├── data/
│           │   ├── AppDatabase.kt
│           │   ├── entity/ (LockedApp.kt, IntruderLog.kt)
│           │   └── dao/ (LockedAppDao.kt, IntruderLogDao.kt)
│           └── util/
│               ├── SecurityUtils.kt
│               ├── PermissionHelper.kt
│               └── RepairModeManager.kt
├── PLAN.md
├── README.md
└── Finish.md  ← File này
```

---

## ⏳ Chờ triển khai (Các bước tiếp theo)

### Bước 2: Màn Hình Khóa & Cơ Chế Chụp Ảnh (Giai đoạn 2)
- [ ] Giao diện nhập PIN overlay (Compose trong WindowManager)
- [ ] Tích hợp CameraX chụp ảnh cam trước ngầm
- [ ] BiometricPrompt cho mở khóa vân tay
- [ ] Kết nối Accessibility → Overlay → Camera

### Bước 3: Giao Diện Quản Lý & Repair Mode (Giai đoạn 3)
- [ ] AppListScreen – Quét & hiển thị toàn bộ app, checkbox khóa
- [ ] IntruderLogScreen – Timeline nhật ký xâm nhập
- [ ] SetupScreen – Thiết lập lần đầu (tạo PIN, cấp quyền)
- [ ] Repair Mode logic hoàn chỉnh

### Bước 4: Bảo Vệ Chuyên Sâu & Đóng Gói (Giai đoạn 4)
- [ ] SettingsScreen hoàn thiện
- [ ] Tối ưu hiệu năng & pin
- [ ] Kiểm thử trên thiết bị thật
- [ ] Đóng gói APK release
