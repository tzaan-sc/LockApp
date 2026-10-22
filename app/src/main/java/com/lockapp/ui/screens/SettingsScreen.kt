package com.lockapp.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockapp.receiver.LockAdminReceiver
import com.lockapp.ui.auth.BiometricAuthHelper
import com.lockapp.util.PermissionHelper
import com.lockapp.util.RepairModeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repairModeManager = remember { RepairModeManager(context) }
    val scrollState = rememberScrollState()

    var isBiometricEnabled by remember { mutableStateOf(repairModeManager.isBiometricEnabled()) }
    var isScramblePinpadEnabled by remember { mutableStateOf(repairModeManager.isScramblePinpadEnabled()) }
    var isCameraEnabled by remember { mutableStateOf(repairModeManager.isCameraCaptureEnabled()) }
    var captureThreshold by remember { mutableIntStateOf(repairModeManager.getCaptureThreshold()) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var isDeviceAdminActive by remember { mutableStateOf(PermissionHelper.isDeviceAdminActive(context)) }
    var isUsbDebuggingOn by remember { mutableStateOf(PermissionHelper.isUsbDebuggingEnabled(context)) }

    val isBiometricHardwareAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    // Dialog đổi mã PIN
    var showChangePinDialog by remember { mutableStateOf(false) }
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }

    // Dialog xác thực PIN khi tắt Device Admin
    var showDisableAdminPinDialog by remember { mutableStateOf(false) }
    var disableAdminPinInput by remember { mutableStateOf("") }
    var disableAdminPinError by remember { mutableStateOf<String?>(null) }

    // Refresh trạng thái khi quay lại
    LaunchedEffect(Unit) {
        isDeviceAdminActive = PermissionHelper.isDeviceAdminActive(context)
        isUsbDebuggingOn = PermissionHelper.isUsbDebuggingEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt & Bảo mật",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // === NHÓM 1: BẢO MẬT & MÃ PIN ===
            SectionHeader(title = "Bảo Mật & Mở Khóa")

            SettingsActionItem(
                icon = Icons.Outlined.Password,
                title = "Đổi mã PIN bảo mật",
                subtitle = "Thay đổi mã PIN 4 số dùng để mở khóa app",
                onClick = {
                    oldPinInput = ""
                    newPinInput = ""
                    confirmNewPinInput = ""
                    changePinError = null
                    showChangePinDialog = true
                }
            )

            SettingsSwitchItem(
                icon = Icons.Outlined.Fingerprint,
                title = "Mở khóa bằng vân tay",
                subtitle = if (isBiometricHardwareAvailable)
                    "Cho phép quét vân tay thay cho mã PIN"
                else
                    "Thiết bị không hỗ trợ sinh trắc học",
                checked = isBiometricEnabled && isBiometricHardwareAvailable,
                enabled = isBiometricHardwareAvailable,
                onCheckedChange = { checked ->
                    isBiometricEnabled = checked
                    repairModeManager.setBiometricEnabled(checked)
                }
            )

            SettingsSwitchItem(
                icon = Icons.Outlined.Shuffle,
                title = "Xáo trộn bàn phím số (Scramble Pinpad)",
                subtitle = "Đảo ngẫu nhiên vị trí các số 0-9 khi mở khóa để chống nhìn trộm mã PIN",
                checked = isScramblePinpadEnabled,
                onCheckedChange = { checked ->
                    isScramblePinpadEnabled = checked
                    repairModeManager.setScramblePinpadEnabled(checked)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // === NHÓM 2: CHỤP ẢNH NGẦM ===
            SectionHeader(title = "Nhật Ký Xâm Nhập")

            SettingsSwitchItem(
                icon = Icons.Outlined.PhotoCamera,
                title = "Tự động chụp ảnh kẻ xâm nhập",
                subtitle = "Chụp ngầm camera trước khi ai đó mở app bị khóa hoặc bấm sai PIN",
                checked = isCameraEnabled,
                onCheckedChange = { checked ->
                    isCameraEnabled = checked
                    repairModeManager.setCameraCaptureEnabled(checked)
                }
            )

            if (isCameraEnabled) {
                val thresholdSubtitle = when (captureThreshold) {
                    0 -> "Chụp ngay khi vừa chạm mở app (Có thể chụp cả chính chủ)"
                    1 -> "Sau 1 lần sai PIN (Khuyên dùng - Không chụp oan)"
                    2 -> "Sau 2 lần sai PIN (Tránh bấm nhầm 1 lần)"
                    3 -> "Sau 3 lần sai PIN (Kiên nhẫn hơn)"
                    else -> "Sau $captureThreshold lần sai PIN"
                }

                SettingsActionItem(
                    icon = Icons.Outlined.Timer,
                    title = "Số lần sai PIN mới chụp ảnh",
                    subtitle = thresholdSubtitle,
                    onClick = {
                        showThresholdDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === NHÓM 3: CHỐNG PHÁ KHÓA & CHỐNG GỠ APP ===
            SectionHeader(title = "Chống Gỡ Cài Đặt & Phá Khóa")

            SettingsSwitchItem(
                icon = Icons.Outlined.AdminPanelSettings,
                title = "Chống gỡ cài đặt (Device Admin)",
                subtitle = if (isDeviceAdminActive)
                    "Đang bảo vệ: Thợ sửa không thể xóa hoặc gỡ app"
                else
                    "Bật để ngăn chặn việc xóa LockApp khi đưa máy đi sửa",
                checked = isDeviceAdminActive,
                onCheckedChange = { checked ->
                    if (checked) {
                        PermissionHelper.requestDeviceAdmin(context)
                    } else {
                        // Yêu cầu PIN trước khi cho phép tắt Device Admin
                        disableAdminPinInput = ""
                        disableAdminPinError = null
                        showDisableAdminPinDialog = true
                    }
                }
            )

            if (isUsbDebuggingOn) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "USB Debugging (ADB) đang BẬT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Hãy vào Cài đặt cho nhà phát triển để tắt USB Debugging nhằm tránh thợ dùng máy tính vượt qua khóa màn hình.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === NHÓM 4: PIN & DỊCH VỤ NGẦM ===
            SectionHeader(title = "Tối Ưu Pin & Chạy Ngầm")

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else true

            SettingsActionItem(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Bỏ qua tối ưu hóa pin (Doze Mode)",
                subtitle = if (isIgnoringBatteryOptimizations)
                    "Đã cấp: Dịch vụ chạy ngầm liên tục 24/7"
                else
                    "Cần cấp để tránh hệ điều hành tắt ngầm dịch vụ bảo vệ",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // === NHÓM 5: THÔNG TIN ỨNG DỤNG ===
            SectionHeader(title = "Thông Tin Ứng Dụng")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "LockApp – Bảo Vệ Riêng Tư",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Phiên bản: 1.0.0 (Native Release)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ứng dụng phát triển phục vụ mục đích bảo vệ máy và dữ liệu cá nhân khi bàn giao điện thoại cho thợ sửa chữa.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // === DIALOG ĐỔI MÃ PIN ===
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Đổi Mã PIN Bảo Mật", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPinInput = it },
                        label = { Text("Mã PIN hiện tại") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("Mã PIN mới (4 số)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPinInput = it },
                        label = { Text("Xác nhận mã PIN mới") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (changePinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = changePinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!repairModeManager.verifyPin(oldPinInput)) {
                            changePinError = "Mã PIN hiện tại không chính xác"
                        } else if (newPinInput.length != 4) {
                            changePinError = "Mã PIN mới phải gồm đủ 4 chữ số"
                        } else if (newPinInput != confirmNewPinInput) {
                            changePinError = "Mã PIN mới không khớp nhau"
                        } else {
                            repairModeManager.changePin(oldPinInput, newPinInput)
                            Toast.makeText(context, "Đã đổi mã PIN thành công!", Toast.LENGTH_SHORT).show()
                            showChangePinDialog = false
                        }
                    }
                ) {
                    Text("Lưu PIN mới")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // === DIALOG XÁC THỰC PIN ĐỂ TẮT DEVICE ADMIN ===
    if (showDisableAdminPinDialog) {
        AlertDialog(
            onDismissRequest = { showDisableAdminPinDialog = false },
            title = { Text("Hủy Quyền Quản Trị", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Nhập mã PIN để xác nhận tắt quyền quản trị thiết bị (sau khi tắt có thể gỡ cài đặt app):",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = disableAdminPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) disableAdminPinInput = it },
                        label = { Text("Mã PIN bảo mật") },
                        singleLine = true,
                        isError = disableAdminPinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (disableAdminPinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = disableAdminPinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repairModeManager.verifyPin(disableAdminPinInput)) {
                            try {
                                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                val component = ComponentName(context, LockAdminReceiver::class.java)
                                dpm.removeActiveAdmin(component)
                                isDeviceAdminActive = false
                                Toast.makeText(context, "Đã hủy quyền quản trị thiết bị", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi hủy quyền: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            showDisableAdminPinDialog = false
                        } else {
                            disableAdminPinError = "Mã PIN không đúng"
                        }
                    }
                ) {
                    Text("Xác nhận hủy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAdminPinDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // === DIALOG CHỌN SỐ LẦN SAI PIN MỚI CHỤP ẢNH ===
    if (showThresholdDialog) {
        val options = listOf(
            1 to "Sau 1 lần sai PIN (Khuyên dùng - Không chụp oan)",
            2 to "Sau 2 lần sai PIN (Tránh chụp khi lỡ bấm nhầm 1 lần)",
            3 to "Sau 3 lần sai PIN (Kiên nhẫn hơn)",
            0 to "Chụp ngay khi vừa chạm mở app (Tối đa, chụp cả chính chủ)"
        )
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Số lần sai PIN mới chụp ảnh", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    captureThreshold = value
                                    repairModeManager.setCaptureThreshold(value)
                                    showThresholdDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = captureThreshold == value,
                                onClick = {
                                    captureThreshold = value
                                    repairModeManager.setCaptureThreshold(value)
                                    showThresholdDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (captureThreshold == value) FontWeight.Bold else FontWeight.Normal,
                                color = if (captureThreshold == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
