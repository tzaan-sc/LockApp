# 📱 Hướng Dẫn Các Cách Chạy & Thử Nghiệm LockApp

> Tài liệu tổng hợp toàn bộ các phương pháp khởi động máy ảo, cài đặt và chạy thử ứng dụng **LockApp** trên máy tính hoặc điện thoại thật.

---

## 📑 Bảng So Sánh Các Cách Chạy

| Cách | Phương pháp | Mức độ dễ | Yêu cầu |
|---|---|---|---|
| **Cách 1** | **Bấm đúp file `Chay_May_Ao.bat`** | ⭐⭐⭐⭐⭐ (Dễ nhất) | Có sẵn máy ảo trên máy |
| **Cách 2** | **Chạy qua Android Studio** | ⭐⭐⭐⭐ (Chuẩn dev) | Đang mở Android Studio |
| **Cách 3** | **Chạy bằng dòng lệnh PowerShell** | ⭐⭐⭐ (Nhanh) | Biết dùng lệnh cơ bản |
| **Cách 4** | **Dùng phần mềm giả lập (LDPlayer, Nox)** | ⭐⭐⭐⭐ (Trực quan) | Cài thêm phần mềm giả lập |
| **Cách 5** | **Cài lên điện thoại Android thật** | ⭐⭐⭐⭐⭐ (Thực tế nhất) | Có điện thoại Android |

---

## 🚀 CÁCH 1: Chạy Tự Động 1-Click Bằng File Batch (Khuyên dùng)

Đây là cách **nhanh nhất và tiện nhất**, mọi thao tác bật máy ảo, cài đặt và mở app đều được tự động hóa 100%.

### Các bước thực hiện:
1. Vào thư mục: **`D:\GIT\LockApp\HDSD\`** (hoặc thư mục gốc dự án).
2. Tìm file **`Chay_May_Ao.bat`** và **nhấp đúp chuột (Double-click)** vào file.
3. Cửa sổ màu đen hiện lên sẽ tự động:
   - 📱 **Bước 1**: Bật cửa sổ điện thoại ảo Android (`Resizable_Experimental`).
   - ⏳ **Bước 2**: Chờ hệ điều hành Android trong máy ảo nạp xong.
   - 📦 **Bước 3**: Tự động cài đặt file `app-release.apk`.
   - 🚀 **Bước 4**: Tự mở app **LockApp** lên ngay trên màn hình máy ảo!

---

## 🛠️ CÁCH 2: Mở & Chạy Trực Tiếp Bằng Android Studio

Dành cho bạn khi đang mở sẵn mã nguồn trong Android Studio, tiện theo dõi Logcat và debug.

### Các bước thực hiện:
1. Mở **Android Studio** với dự án tại `D:\GIT\LockApp`.
2. **Bật máy ảo (Device Manager)**:
   - Trên thanh menu trên cùng: Chọn **Tools** ➔ **Device Manager**.
   - (Hoặc bấm biểu tượng chiếc điện thoại 📱 ở mép phải màn hình).
   - Tìm máy ảo có tên **`Resizable_Experimental`** ➔ bấm nút **Play (▶ hình tam giác)** ở cột Actions.
   - Cửa sổ điện thoại ảo sẽ xuất hiện trên màn hình.
3. **Chạy app lên máy ảo**:
   - Ở thanh công cụ trên cùng của Android Studio:
     - Ô thiết bị: Đảm bảo chọn đúng máy ảo vừa bật.
     - Ô module: Đảm bảo chọn là **`app`**.
   - Bấm nút **Run (▶ tam giác màu xanh lá cây)** hoặc bấm phím tắt **`Shift + F10`**.
   - Android Studio sẽ build và tự động mở LockApp trên máy ảo.

---

## 💻 CÁCH 3: Chạy Bằng Dòng Lệnh (PowerShell / Command Prompt)

Dành cho bạn muốn điều khiển linh hoạt qua terminal.

### 1. Lệnh bật máy ảo:
Mở PowerShell và dán dòng lệnh sau:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Resizable_Experimental
```

### 2. Lệnh cài đặt file APK vào máy ảo:
Khi máy ảo đã mở lên màn hình chính:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "D:\GIT\LockApp\app\build\outputs\apk\release\app-release.apk"
```

*(Hoặc bạn chỉ cần dùng chuột **kéo file `app-release.apk` thả thẳng vào màn hình máy ảo** là app tự động cài đặt).*

---

## 🎮 CÁCH 4: Dùng Các Phần Mềm Giả Lập Ngoài (LDPlayer, Nox, BlueStacks)

Nếu máy tính yếu hoặc bạn thích giao diện giả lập trực quan giống máy tính bảng:

1. **Tải phần mềm giả lập**:
   - **LDPlayer 9** (Rất nhẹ, mượt): Tải tại [ldplayer.net](https://vi.ldplayer.net/)
   - **NoxPlayer** hoặc **BlueStacks 5**.
2. **Cài app vào giả lập**:
   - Mở LDPlayer / NoxPlayer lên.
   - Kéo file `D:\GIT\LockApp\app\build\outputs\apk\release\app-release.apk` thả vào cửa sổ giả lập.
   - Biểu tượng app **LockApp** sẽ xuất hiện trên màn hình.
3. **Mẹo test camera**: LDPlayer có thể tự động nhận webcam của laptop/máy tính làm camera trước để test tính năng chụp ảnh ngầm kẻ xâm nhập!

---

## 📱 CÁCH 5: Cài Đặt & Test Trên Điện Thoại Thật

Đây là trải nghiệm chuẩn xác nhất với cảm biến vân tay và camera thật của bạn.

### Các bước thực hiện:
1. **Chép file APK vào điện thoại**:
   - File APK: `D:\GIT\LockApp\app\build\outputs\apk\release\app-release.apk` (Chỉ **3.3 MB**).
   - Chép qua cáp USB, hoặc gửi vào mục **"Cloud của tôi"** trên Zalo, hoặc tải lên Google Drive rồi mở điện thoại tải về.
2. **Cài đặt**:
   - Bấm vào file `app-release.apk` trên điện thoại để Cài đặt.
   - Nếu máy hỏi *"Cho phép cài đặt ứng dụng không rõ nguồn gốc"*, hãy bấm **Cho phép (Allow)**.
3. **Thiết lập & Cấp quyền**:
   - Mở **LockApp** ➔ Tạo mã PIN 4 số.
   - Cấp 4 quyền bảo vệ: **Vẽ đè (Overlay)**, **Trợ năng (Accessibility)**, **Camera ngầm**, **Quản trị viên (Device Admin)**.
4. **Trải nghiệm**:
   - Gạt nút **"Chế Độ Sửa Máy"** sang **BẬT**.
   - Mở thử Zalo, Facebook, Thư viện ảnh để thấy màn hình khóa bật lên tức thì và camera trước âm thầm chụp ảnh kẻ mở app!

*(Mẹo: Bạn có thể cắm cáp USB nối điện thoại với máy tính và dùng công cụ **`scrcpy`** để chiếu màn hình điện thoại lên máy tính, điều khiển bằng chuột cực kỳ tiện).*

---

## 📍 Vị Trí Các File Quan Trọng

- 📦 **File Release APK (Đã tối ưu, 3.3 MB)**:
  `D:\GIT\LockApp\app\build\outputs\apk\release\app-release.apk`
- ⚡ **File chạy tự động 1-Click**:
  `D:\GIT\LockApp\HDSD\Chay_May_Ao.bat`
- 📄 **Tài liệu hoàn thành dự án**:
  `D:\GIT\LockApp\docs\Finish.md`
