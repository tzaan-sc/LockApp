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
│           │   ├── auth/ (BiometricAuthHelper.kt, BiometricPromptActivity.kt)
│           │   └── screens/
│           │       ├── HomeScreen.kt
│           │       ├── LockOverlayScreen.kt
│           │       ├── AppListScreen.kt
│           │       ├── IntruderLogScreen.kt
│           │       └── SetupScreen.kt
│           ├── service/
│           │   ├── LockService.kt
│           │   ├── LockAccessibilityService.kt
│           │   ├── LockOverlayManager.kt
│           │   └── OverlayLifecycleOwner.kt
│           ├── camera/
│           │   └── IntruderCaptureManager.kt
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

## ✅ Bước 2: Màn Hình Khóa & Cơ Chế Chụp Ảnh Ngầm (Giai đoạn 2)

**Trạng thái:** ✅ Đã hoàn thành & Build APK thành công (`app-debug.apk`)!

### Đã hoàn thành trong Bước 2:

1. **Giao diện nhập PIN Overlay (`LockOverlayScreen.kt`)**:
   - Thiết kế giao diện Jetpack Compose toàn màn hình phong cách Dark Slate sang trọng, hiện đại.
   - Hiển thị động biểu tượng icon và tên ứng dụng đang bị khóa (lấy trực tiếp từ `PackageManager` & Coil).
   - Chỉ báo PIN dots (4 số) với hiệu ứng đổi màu.
   - Bàn phím số Numpad cảm ứng (0-9) cùng phản hồi xúc giác (Haptic Feedback) sống động.
   - Hiệu ứng lắc rung ngang (Shake Animation) với viền đỏ khi nhập sai mã PIN.
   - Nút "Trở về màn hình chính" (Home) giúp người dùng thoát an toàn về màn hình chính điện thoại nếu không nhớ mã PIN.

2. **Compose trong WindowManager (`OverlayLifecycleOwner.kt` & `LockOverlayManager.kt`)**:
   - Triển khai custom `OverlayLifecycleOwner` (kết hợp `LifecycleOwner`, `SavedStateRegistryOwner`, `ViewModelStoreOwner`) để chạy Jetpack Compose mượt mà trực tiếp trên `WindowManager` của Background Service.
   - `LockOverlayManager`: Sử dụng `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, chặn hoàn toàn tương tác vào app bên dưới khi đang bị khóa.

3. **Chụp ảnh ngầm CameraX (`IntruderCaptureManager.kt`)**:
   - Sử dụng CameraX API với camera trước (`CameraSelector.DEFAULT_FRONT_CAMERA`).
   - Chụp ảnh ngầm hoàn toàn: **không mở màn hình camera preview, không bật đèn flash, không phát âm thanh chụp**.
   - Lưu ảnh an toàn vào bộ nhớ nội bộ riêng của ứng dụng (`context.filesDir/intruders/`), không xuất hiện trong Gallery công khai.
   - Tự động ghi nhật ký xâm nhập vào Room Database (`IntruderLog`) gồm: đường dẫn ảnh, thời gian, tên app, package name, số lần nhập sai.
   - Hỗ trợ rung phản hồi cảnh báo bằng `Vibrator` / `VibratorManager`.

4. **Xác thực sinh trắc học vân tay (`BiometricAuthHelper.kt` & `BiometricPromptActivity.kt`)**:
   - Kiểm tra hỗ trợ cảm biến vân tay trên thiết bị bằng `BiometricManager`.
   - Tạo `BiometricPromptActivity` với theme trong suốt (`Theme.LockApp.Translucent`) để hiển thị hộp thoại vân tay gốc của Android.
   - Mở khóa thành công bằng vân tay sẽ ngay lập tức mở app và đóng overlay.

5. **Kết nối hoàn chỉnh luồng hoạt động (`LockAccessibilityService.kt`)**:
   - Khi phát hiện người dùng chạm vào app bị khóa (< 15ms):
     1. Kích hoạt `LockOverlayManager.showLockOverlay(packageName)` che kín app.
     2. Tự động chụp ảnh mặt người cầm máy bằng `IntruderCaptureManager.captureIntruder()`.
   - Quản lý phiên tạm thời (`tempUnlockedPackage`): Khi người dùng nhập đúng PIN hoặc vân tay, cho phép sử dụng bình thường; khi chuyển sang app khác hoặc về Home, trạng thái khóa sẽ tự động kích hoạt lại.

6. **Mật khẩu dự phòng ban đầu (`RepairModeManager.kt`)**:
   - Tích hợp PIN mặc định ban đầu là `1234` để kiểm thử ngay lập tức trước khi cài đặt mật khẩu riêng.

---

## ✅ Bước 3: Giao Diện Quản Lý & Chế Độ Sửa Máy (Giai đoạn 3)

**Trạng thái:** ✅ Đã hoàn thành & Build APK thành công (`app-debug.apk`)!

### Đã hoàn thành trong Bước 3:

1. **Màn hình Quản lý ứng dụng khóa (`AppListScreen.kt`)**:
   - Quét toàn bộ ứng dụng cài đặt trên thiết bị bằng `PackageManager` (kèm app Cài đặt hệ thống).
   - Tự động nhận diện và gắn nhãn "Nhạy cảm" cho các app quan trọng: Zalo, Messenger, Facebook, Google Photos, Album ảnh, Tin nhắn SMS, CH Play, Cài đặt...
   - Phân loại danh mục theo tab: **Tất cả | Đang khóa | Mạng xã hội | Ảnh & Video | Tài chính | Hệ thống**.
   - Thanh tìm kiếm ứng dụng thời gian thực.
   - Công tắc (Switch) chọn khóa/mở khóa từng ứng dụng, lưu trực tiếp và đồng bộ vào Room Database (`LockedAppDao`).
   - Nút hành động nhanh **"Khóa đề xuất"**: tự động khóa ngay lập tức toàn bộ app nhạy cảm.

2. **Màn hình Nhật ký xâm nhập (`IntruderLogScreen.kt`)**:
   - Danh sách timeline trực quan kết nối trực tiếp với Room Database (`IntruderLogDao.getAllLogs()`).
   - Hiển thị ảnh chụp khuôn mặt kẻ xâm nhập (thumbnail tỉ lệ chuẩn), tên ứng dụng bị truy cập, thời gian chi tiết (`HH:mm:ss - dd/MM/yyyy`) và số lần nhập sai PIN.
   - Hộp thoại **xem chi tiết ảnh phóng to**: nhấn vào ảnh bất kỳ để xem toàn bộ thông tin và ảnh chất lượng cao.
   - Tính năng xóa từng bản ghi hoặc nút **"Xóa tất cả"** trên thanh tiêu đề (tự động dọn sạch cả database lẫn file ảnh trong storage).
   - Trạng thái trống (Empty State) với biểu tượng khiên bảo vệ xanh lá khi thiết bị an toàn.

3. **Màn hình Thiết lập lần đầu Onboarding (`SetupScreen.kt`)**:
   - Hướng dẫn thiết lập 2 bước thân thiện:
     - **Bước 1**: Tạo mã PIN bảo mật cá nhân (4 số) + xác nhận lại mã PIN.
     - **Bước 2**: Danh sách cấp quyền hệ thống trực quan với tích xanh real-time:
       - Quyền Vẽ đè (Overlay)
       - Quyền Hỗ trợ tiếp cận (Accessibility Service)
       - Quyền Camera ngầm
       - Quyền Quản trị viên thiết bị (Device Admin - chống gỡ cài đặt)
   - Lưu trữ mã PIN đã băm bảo mật (PBKDF2) qua `RepairModeManager` và kích hoạt trạng thái sẵn sàng bảo vệ.

4. **Nâng cấp Logic Chế độ Sửa máy (Repair Mode) trên `HomeScreen.kt`**:
   - Hiển thị số liệu thời gian thực: **Số app đang bảo vệ** và **Số lần phát hiện xâm nhập** qua Flow Room Database.
   - Khi **BẬT Repair Mode** (1 chạm): Tự động khóa toàn bộ app mặc định nhạy cảm và khởi chạy Foreground Service bảo vệ.
   - Khi **TẮT Repair Mode**: Bắt buộc hiển thị hộp thoại xác thực mã PIN bảo mật. Chỉ khi nhập đúng PIN mới cho phép tắt bảo vệ. Nếu nhập sai, tự động rung cảnh báo và chụp ảnh kẻ đang cố tắt bảo vệ.

5. **Điều hướng toàn diện (`Navigation.kt`)**:
   - Tự động kiểm tra `isSetupComplete()` để chuyển người dùng vào `SetupScreen` (nếu mở lần đầu) hoặc `HomeScreen` (nếu đã thiết lập).
   - Kết nối mượt mà giữa các màn hình `Home` ↔ `AppList` ↔ `IntruderLog` ↔ `Setup`.

---

## ⏳ Chờ triển khai (Các bước tiếp theo)

### Bước 4: Bảo Vệ Chuyên Sâu & Đóng Gói (Giai đoạn 4)
- [ ] SettingsScreen hoàn thiện (Đổi mã PIN, bật/tắt chụp ảnh ngầm, bật/tắt vân tay)
- [ ] Tối ưu hóa hiệu năng & thời lượng pin
- [ ] Kiểm thử toàn diện trên thiết bị thật
- [ ] Đóng gói APK release tối ưu hóa ProGuard/R8


