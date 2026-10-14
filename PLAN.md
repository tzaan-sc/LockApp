# 📋 Kế Hoạch Phát Triển LockApp

> **Tên dự án:** LockApp – Khóa Ứng Dụng & Bảo Vệ Dữ Liệu Khi Đưa Máy Đi Sửa
> **Công nghệ:** Android Native (Kotlin + Jetpack Compose)
> **Mục đích:** Sử dụng cá nhân / nội bộ (cài trực tiếp APK, không qua Play Store)
> **Ngày bắt đầu:** 14/10/2026

---

## 1. Tổng Quan Dự Án

### 1.1 Vấn đề cần giải quyết
Khi đưa điện thoại đi sửa, thợ sửa có thể truy cập vào các ứng dụng cá nhân (Zalo, Tin nhắn, Album ảnh, Ngân hàng...) mà chủ máy không hề biết. Cần một ứng dụng:
- Khóa toàn bộ app nhạy cảm bằng mật khẩu riêng
- Chỉ cần **1 chạm** để bật "Chế độ sửa máy"
- **Chụp ảnh mặt ngầm** bất kỳ ai cố mở app bị khóa
- Chống gỡ cài đặt, chống vượt qua bảo vệ

### 1.2 Tại sao chọn Android Native (Kotlin)?
| Tiêu chí | Native (Kotlin) | Flutter |
|---|---|---|
| Tốc độ hiển thị overlay khóa | < 15ms ✅ | 50-100ms (có thể lộ nội dung) |
| Chụp ảnh ngầm bằng CameraX | Trực tiếp, mượt ✅ | Phải qua bridge, có độ trễ |
| Foreground Service & Accessibility | Kiểm soát hoàn toàn ✅ | Hạn chế, dễ bị kill |
| Kích thước APK | ~5-8 MB ✅ | ~15-25 MB |
| Tối ưu pin | Tốt hơn ✅ | Tốn hơn do engine Dart |

---

## 2. Danh Sách Tính Năng

### 2.1 Tính năng cốt lõi (Core)

#### A. Khóa ứng dụng (App Lock)
- [ ] Phát hiện khi người dùng mở một app đang bị khóa (qua `UsageStatsManager` hoặc `AccessibilityService`)
- [ ] Hiển thị **màn hình khóa overlay** đè lên app ngay lập tức (quyền `SYSTEM_ALERT_WINDOW`)
- [ ] Hỗ trợ nhập **mã PIN 4-6 số** hoặc **mật khẩu chữ + số**
- [ ] Hỗ trợ mở khóa bằng **vân tay / sinh trắc học** (BiometricPrompt API)
- [ ] Dịch vụ chạy ngầm liên tục (`Foreground Service`) với notification nhỏ

#### B. Chế độ Sửa Máy (Repair Mode) – 1 Chạm
- [ ] **Nút gạt (Switch) trên màn hình chính** – bật/tắt toàn bộ bảo vệ chỉ 1 chạm
- [ ] Khi **BẬT Repair Mode:**
  - Tự động khóa danh sách app mặc định: `Cài đặt`, `Play Store`, `Zalo`, `Messenger`, `Album Ảnh`, `SMS`, `Email`, `Ngân hàng`, `Ghi chú`...
  - Chỉ cho phép truy cập: `Điện thoại (Phone)`, `Camera` (để test), `Loa/Nhạc`
- [ ] Khi **TẮT Repair Mode:** Mở khóa toàn bộ, yêu cầu nhập mật khẩu chính trước
- [ ] **Danh sách tùy chỉnh:** Cho phép người dùng:
  - ✅ Tích thêm app muốn khóa
  - ❌ Bỏ tích app không cần khóa
  - Phân loại theo nhóm: `Mạng xã hội`, `Tin nhắn`, `Ảnh/Video`, `Tài chính`, `Hệ thống`, `Khác`

#### C. Chụp Ảnh Kẻ Xâm Nhập (Intruder Camera Log)
- [ ] **Mỗi lần** ai đó chạm vào app bị khóa → camera trước **tự động chụp ngầm 1 ảnh**
  - Không bật đèn flash
  - Không phát âm thanh chụp
  - Sử dụng `CameraX` API, chụp trong < 0.5 giây
- [ ] Lưu **nhật ký xâm nhập** gồm:
  - 📷 Ảnh mặt người cầm máy
  - 🕐 Thời gian chính xác (ngày/giờ/phút/giây)
  - 📱 Tên app bị cố gắng mở (VD: "Zalo", "Album Ảnh")
  - ❌ Số lần nhập sai mật khẩu (nếu có)
- [ ] Nhật ký này **chỉ xem được** sau khi nhập mật khẩu chính của LockApp
- [ ] Giao diện xem nhật ký dạng **timeline** với thumbnail ảnh

#### D. Chống Phá Khóa (Anti-Bypass)
- [ ] **Chống gỡ cài đặt:** Kích hoạt quyền `Device Administrator` → thợ không thể kéo xóa app
- [ ] **Khóa app Cài đặt (Settings):** Ngăn thợ vào Settings → Ứng dụng → Buộc dừng/Gỡ quyền LockApp
- [ ] **Cảnh báo USB Debugging:** Kiểm tra `Settings.Global.ADB_ENABLED`, nếu đang bật → hiển thị cảnh báo + hướng dẫn tắt trước khi đưa máy đi sửa

---

## 3. Kiến Trúc Kỹ Thuật

### 3.1 Cấu trúc thư mục dự án
```
LockApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/lockapp/
│   │   │   ├── MainActivity.kt              # Màn hình chính (Jetpack Compose)
│   │   │   ├── ui/
│   │   │   │   ├── theme/                    # Material3 Theme
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt         # Trang chủ + Nút Repair Mode
│   │   │   │   │   ├── AppListScreen.kt      # Danh sách app để chọn khóa
│   │   │   │   │   ├── IntruderLogScreen.kt  # Xem nhật ký xâm nhập
│   │   │   │   │   ├── SettingsScreen.kt     # Cài đặt app (đổi PIN, v.v.)
│   │   │   │   │   └── SetupScreen.kt        # Thiết lập lần đầu (tạo PIN)
│   │   │   │   └── components/               # UI components tái sử dụng
│   │   │   ├── service/
│   │   │   │   ├── LockService.kt            # Foreground Service chạy ngầm
│   │   │   │   ├── LockAccessibilityService.kt # Phát hiện app được mở
│   │   │   │   └── LockOverlayManager.kt     # Quản lý hiển thị overlay khóa
│   │   │   ├── camera/
│   │   │   │   └── IntruderCaptureManager.kt # CameraX – chụp ảnh ngầm
│   │   │   ├── data/
│   │   │   │   ├── AppDatabase.kt            # Room Database
│   │   │   │   ├── dao/
│   │   │   │   │   ├── LockedAppDao.kt       # DAO cho danh sách app bị khóa
│   │   │   │   │   └── IntruderLogDao.kt     # DAO cho nhật ký xâm nhập
│   │   │   │   ├── entity/
│   │   │   │   │   ├── LockedApp.kt          # Entity: app bị khóa
│   │   │   │   │   └── IntruderLog.kt        # Entity: bản ghi xâm nhập
│   │   │   │   └── repository/
│   │   │   │       ├── AppRepository.kt
│   │   │   │       └── LogRepository.kt
│   │   │   ├── receiver/
│   │   │   │   ├── LockAdminReceiver.kt      # Device Administrator receiver
│   │   │   │   └── BootReceiver.kt           # Tự khởi động sau khi reboot
│   │   │   └── util/
│   │   │       ├── SecurityUtils.kt          # Mã hóa, hash password
│   │   │       ├── PermissionHelper.kt       # Quản lý xin quyền
│   │   │       └── RepairModeManager.kt      # Logic bật/tắt chế độ sửa máy
│   │   ├── res/
│   │   │   ├── xml/
│   │   │   │   ├── device_admin_policies.xml # Cấu hình Device Admin
│   │   │   │   └── accessibility_config.xml  # Cấu hình Accessibility Service
│   │   │   └── ...
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── PLAN.md                                    # File này
└── README.md
```

### 3.2 Luồng hoạt động chính

```
┌─────────────────────────────────────────────────────────────┐
│                    LUỒNG KHÓA ỨNG DỤNG                      │
│                                                              │
│  Người dùng mở Zalo                                         │
│       │                                                      │
│       ▼                                                      │
│  AccessibilityService phát hiện (< 15ms)                     │
│       │                                                      │
│       ▼                                                      │
│  Kiểm tra: Zalo có trong danh sách khóa không?               │
│       │                                                      │
│    CÓ ──────────────────┐                                    │
│       │                  │                                    │
│       ▼                  ▼                                    │
│  Hiển thị Overlay    CameraX chụp ảnh                        │
│  (Màn hình nhập PIN)  cam trước ngầm                         │
│       │                  │                                    │
│       │                  ▼                                    │
│       │             Lưu ảnh + log                             │
│       │             vào Room DB                               │
│       ▼                                                      │
│  Người dùng nhập PIN                                         │
│       │                                                      │
│   ĐÚNG ──► Ẩn overlay, cho phép dùng app                     │
│   SAI  ──► Rung, hiển thị "Sai mật khẩu", chụp thêm 1 ảnh  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Công nghệ & Thư viện sử dụng

| Thư viện | Phiên bản | Mục đích |
|---|---|---|
| Kotlin | 1.9.x | Ngôn ngữ chính |
| Jetpack Compose + Material 3 | 1.6.x / 1.2.x | Giao diện UI |
| CameraX | 1.3.x | Chụp ảnh cam trước ngầm |
| Room Database | 2.6.x | Lưu trữ danh sách app khóa + nhật ký |
| EncryptedSharedPreferences | 1.1.x | Lưu mật khẩu mã hóa |
| BiometricPrompt | 1.2.x | Mở khóa bằng vân tay |
| Hilt (Dagger) | 2.50.x | Dependency Injection |
| Coil | 2.6.x | Hiển thị ảnh (icon app, ảnh intruder) |

---

## 4. Lịch Trình Chi Tiết (8 Tuần)

### 🔵 Tuần 1-2: Giai Đoạn 1 – Khởi Tạo Dự Án & Dịch Vụ Ngầm

**Tuần 1:**
- [ ] Tạo project Android Studio (Kotlin + Jetpack Compose + Material 3)
- [ ] Cấu hình `build.gradle.kts` với toàn bộ dependencies
- [ ] Khai báo permissions trong `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
  <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
      tools:ignore="ProtectedPermissions"/>
  <uses-permission android:name="android.permission.CAMERA"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
      tools:ignore="QueryAllPackagesPermission"/>
  ```
- [ ] Tạo `LockService.kt` – Foreground Service với persistent notification
- [ ] Tạo `BootReceiver.kt` – tự động khởi động service khi máy reboot

**Tuần 2:**
- [ ] Tạo `LockAccessibilityService.kt`:
  - Lắng nghe sự kiện `TYPE_WINDOW_STATE_CHANGED`
  - So sánh `packageName` với danh sách app bị khóa
  - Gọi `LockOverlayManager` nếu phát hiện app cần khóa
- [ ] Tạo `LockOverlayManager.kt`:
  - Dùng `WindowManager.addView()` với `TYPE_APPLICATION_OVERLAY`
  - Hiển thị view nhập PIN che toàn bộ màn hình
- [ ] Viết test: mô phỏng mở app → verify overlay hiện đúng

**Kết quả tuần 1-2:** Service chạy ngầm ổn định, phát hiện app mở và hiển thị màn hình khóa.

---

### 🟢 Tuần 3-4: Giai Đoạn 2 – Màn Hình Khóa & Chụp Ảnh Ngầm

**Tuần 3:**
- [ ] Thiết kế UI **màn hình khóa** (Compose trong Overlay View):
  - Bàn phím số PIN (numpad đẹp, có animation)
  - Hiển thị icon + tên app đang bị khóa
  - Nút mở bằng vân tay
  - Animation rung khi nhập sai
- [ ] Tích hợp `BiometricPrompt` cho mở khóa vân tay
- [ ] `SecurityUtils.kt`: hash mật khẩu bằng `Argon2` hoặc `PBKDF2`
- [ ] `EncryptedSharedPreferences` để lưu hash password an toàn

**Tuần 4:**
- [ ] Tạo `IntruderCaptureManager.kt`:
  - Khởi tạo `ProcessCameraProvider` trong Service context
  - Khi overlay hiện lên → bind camera trước với `ImageCapture` use case
  - Chụp 1 ảnh ngầm (không preview, không flash, không âm thanh)
  - Unbind camera ngay sau khi chụp xong
- [ ] Tạo Room Database:
  - `IntruderLog` entity: `id`, `photoPath`, `timestamp`, `targetAppPackage`, `targetAppName`, `wrongAttempts`
  - `LockedApp` entity: `packageName`, `appName`, `iconUri`, `isLocked`, `category`
- [ ] Lưu ảnh vào `app internal storage` (không hiện trong Gallery)

**Kết quả tuần 3-4:** Màn hình khóa đẹp, chụp ảnh ngầm hoạt động, dữ liệu lưu an toàn.

---

### 🟡 Tuần 5-6: Giai Đoạn 3 – Giao Diện Quản Lý & Repair Mode

**Tuần 5:**
- [ ] `HomeScreen.kt` – Màn hình chính:
  - **Nút gạt lớn "Chế Độ Sửa Máy"** ở trên cùng (nổi bật, dễ thấy)
  - Hiển thị trạng thái: "Đang bảo vệ X ứng dụng"
  - Số lượng lần xâm nhập được phát hiện
  - Quick actions: Xem nhật ký, Cài đặt
- [ ] `AppListScreen.kt` – Danh sách ứng dụng:
  - Quét toàn bộ app đã cài bằng `PackageManager`
  - Hiển thị icon + tên app
  - Checkbox để chọn/bỏ chọn khóa
  - Phân loại theo tab: `Mạng xã hội` | `Tin nhắn` | `Ảnh` | `Tài chính` | `Hệ thống` | `Tất cả`
  - Thanh tìm kiếm

**Tuần 6:**
- [ ] `RepairModeManager.kt`:
  - Khi bật: load danh sách mặc định + danh sách tùy chỉnh → bật khóa toàn bộ
  - Khi tắt: yêu cầu nhập mật khẩu chính → tắt khóa toàn bộ
  - Lưu trạng thái vào `SharedPreferences`
- [ ] `IntruderLogScreen.kt` – Nhật ký xâm nhập:
  - Danh sách dạng timeline (mới nhất ở trên)
  - Mỗi mục: Ảnh thumbnail + Tên app + Thời gian + Số lần sai PIN
  - Bấm vào xem chi tiết (ảnh to, thông tin đầy đủ)
  - Nút xóa từng mục hoặc xóa tất cả
- [ ] `SetupScreen.kt` – Thiết lập lần đầu:
  - Tạo mật khẩu chính
  - Hướng dẫn cấp quyền (từng bước có hình minh họa)

**Kết quả tuần 5-6:** Giao diện hoàn chỉnh, Repair Mode 1 chạm hoạt động, xem nhật ký trực quan.

---

### 🔴 Tuần 7-8: Giai Đoạn 4 – Bảo Vệ Chuyên Sâu & Đóng Gói

**Tuần 7:**
- [ ] `LockAdminReceiver.kt` + `device_admin_policies.xml`:
  - Đăng ký Device Administrator
  - Khi được kích hoạt: ngăn chặn gỡ cài đặt LockApp
  - UI hướng dẫn người dùng bật quyền admin
- [ ] Khóa app **Cài đặt (Settings)** – thêm `com.android.settings` vào danh sách bắt buộc khóa khi Repair Mode bật
- [ ] Kiểm tra & cảnh báo **USB Debugging**:
  ```kotlin
  val adbEnabled = Settings.Global.getInt(
      contentResolver,
      Settings.Global.ADB_ENABLED, 0
  )
  if (adbEnabled == 1) showUsbWarningDialog()
  ```

**Tuần 8:**
- [ ] `SettingsScreen.kt` hoàn thiện:
  - Đổi mật khẩu chính
  - Bật/tắt vân tay
  - Xóa toàn bộ nhật ký
  - Thông tin ứng dụng (phiên bản, hướng dẫn sử dụng)
- [ ] Tối ưu hiệu năng & pin
- [ ] Kiểm thử toàn diện trên thiết bị thật
- [ ] Tạo **release build**:
  ```bash
  ./gradlew assembleRelease
  ```
- [ ] Ký APK bằng keystore nội bộ
- [ ] Viết hướng dẫn cài đặt trong `README.md`

**Kết quả tuần 7-8:** APK hoàn chỉnh, sẵn sàng cài đặt và sử dụng.

---

## 5. Quyền Hệ Thống Cần Thiết

| Quyền | Mục đích | Cách xin |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Vẽ overlay màn hình khóa đè lên app khác | Settings → Ứng dụng → LockApp → Hiển thị trên ứng dụng khác |
| `PACKAGE_USAGE_STATS` | Phát hiện app nào đang mở | Settings → Bảo mật → Quyền truy cập sử dụng |
| `BIND_ACCESSIBILITY_SERVICE` | Phát hiện nhanh khi app được mở | Settings → Trợ năng → LockApp |
| `CAMERA` | Chụp ảnh ngầm cam trước | Runtime permission dialog |
| `FOREGROUND_SERVICE` | Giữ service chạy liên tục | Tự động (manifest) |
| `RECEIVE_BOOT_COMPLETED` | Tự khởi động sau khi reboot | Tự động (manifest) |
| `QUERY_ALL_PACKAGES` | Quét danh sách toàn bộ app đã cài | Tự động (manifest, không cần Play Store nên OK) |
| `BIND_DEVICE_ADMIN` | Chống gỡ cài đặt | Settings → Bảo mật → Quản trị viên thiết bị |

---

## 6. Rủi Ro & Giải Pháp

| Rủi ro | Mức độ | Giải pháp |
|---|---|---|
| Hệ điều hành tắt ngầm service (Battery Optimization) | Cao | Hướng dẫn tắt tối ưu pin cho LockApp, dùng `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| Một số ROM (Xiaomi, Oppo, Vivo) chặn Accessibility Service | Cao | Hướng dẫn chi tiết theo từng hãng (Autostart, Battery saver) |
| Thợ sửa khởi động vào Safe Mode (vô hiệu hóa app bên thứ 3) | Trung bình | Không có giải pháp phần mềm 100%, nhưng đa số thợ không biết cách này |
| Thợ factory reset máy | Thấp | Ngoài tầm kiểm soát – cần backup trước khi đưa máy |
| Camera trước bị che / tối | Trung bình | Vẫn lưu ảnh + timestamp để có bằng chứng thời gian |

---

## 7. Thiết Bị Test

- **Thiết bị chính:** Điện thoại Android cá nhân (Android 10+)
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 34 (Android 14)

---

## 8. Cách Cài Đặt APK (Hướng Dẫn Cho Người Dùng)

1. Sao chép file `lockapp-release.apk` vào điện thoại
2. Mở **Cài đặt → Bảo mật → Cho phép cài từ nguồn không xác định**
3. Mở file APK → Cài đặt
4. Khởi động LockApp → **Tạo mật khẩu chính**
5. Làm theo hướng dẫn cấp quyền (Overlay, Accessibility, Camera, Device Admin)
6. Chọn các app muốn khóa hoặc bật **Chế độ sửa máy**
7. ✅ Xong! Điện thoại đã được bảo vệ

---

*📌 Ghi chú: File kế hoạch này sẽ được cập nhật liên tục theo tiến độ thực tế.*
