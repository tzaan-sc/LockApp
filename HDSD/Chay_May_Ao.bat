@echo off
chcp 65001 >nul
title Khởi động Máy Ảo Android & Cài đặt LockApp

echo ===================================================
echo   ĐANG KHỞI ĐỘNG MÁY ẢO ANDROID (Resizable_Experimental)
echo ===================================================
echo.

set EMULATOR="%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"
set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set APK="D:\GIT\LockApp\app\build\outputs\apk\release\app-release.apk"

echo [1/3] Đang bật cửa sổ máy ảo... Vui lòng đợi trong giây lát...
start "" %EMULATOR% -avd Resizable_Experimental

echo [2/3] Đang chờ hệ điều hành Android khởi động xong...
%ADB% wait-for-device

:wait_boot
%ADB% shell getprop sys.boot_completed 2>nul | find "1" >nul
if errorlevel 1 (
    timeout /t 3 /nobreak >nul
    goto wait_boot
)

echo [3/3] Android đã sẵn sàng! Đang tự động cài đặt LockApp...
%ADB% install -r %APK%

echo.
echo Đang mở ứng dụng LockApp trên máy ảo...
%ADB% shell monkey -p com.lockapp -c android.intent.category.LAUNCHER 1 >nul 2>&1

echo.
echo ===================================================
echo   CÀI ĐẶT THÀNH CÔNG! BẠN CÓ THỂ TEST APP TRÊN MÁY ẢO!
echo ===================================================
echo.
pause
