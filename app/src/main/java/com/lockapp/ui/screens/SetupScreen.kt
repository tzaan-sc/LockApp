package com.lockapp.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockapp.util.PermissionHelper
import com.lockapp.util.RepairModeManager
import com.lockapp.util.SecurityUtils

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val repairModeManager = remember { RepairModeManager(context) }
    val scrollState = rememberScrollState()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableIntStateOf(1) } // 1: Tạo PIN, 2: Cấp quyền

    // Trạng thái quyền hệ thống
    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var hasCameraPermission by remember { mutableStateOf(PermissionHelper.hasCameraPermission(context)) }
    var hasAdminPermission by remember { mutableStateOf(PermissionHelper.isDeviceAdminActive(context)) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Refresh trạng thái khi quay lại app
    LaunchedEffect(Unit) {
        hasOverlayPermission = PermissionHelper.canDrawOverlays(context)
        hasAccessibilityPermission = PermissionHelper.isAccessibilityServiceEnabled(context)
        hasCameraPermission = PermissionHelper.hasCameraPermission(context)
        hasAdminPermission = PermissionHelper.isDeviceAdminActive(context)
    }

    fun validateAndProceedToPermissions() {
        if (pin.length != 4) {
            pinError = "Vui lòng nhập đủ 4 chữ số PIN"
            return
        }
        if (pin != confirmPin) {
            pinError = "Mã PIN xác nhận không trùng khớp"
            return
        }
        pinError = null
        currentStep = 2
    }

    fun finishSetup() {
        val (hash, salt) = SecurityUtils.hashPassword(pin)
        repairModeManager.savePinHash(hash, salt)
        repairModeManager.setSetupComplete(true)
        onSetupComplete()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Icon Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Thiết Lập LockApp",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                if (currentStep == 1) "Bước 1/2: Tạo mã PIN bảo mật cá nhân"
                else "Bước 2/2: Cấp các quyền bảo vệ cần thiết",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (currentStep == 1) {
                // === STEP 1: CREATE PIN ===
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Mã PIN này dùng để mở khóa các app bị chặn và tắt chế độ bảo vệ khi nhận lại máy.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                            label = { Text("Nhập mã PIN mới (4 số)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPin = it },
                            label = { Text("Xác nhận lại mã PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        AnimatedVisibility(visible = pinError != null) {
                            Text(
                                text = pinError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { validateAndProceedToPermissions() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tiếp tục sang Cấp quyền", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // === STEP 2: GRANT PERMISSIONS ===
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PermissionCard(
                        title = "Quyền vẽ đè (Overlay)",
                        description = "Cần thiết để hiển thị màn hình khóa che ứng dụng bị khóa.",
                        isGranted = hasOverlayPermission,
                        onGrant = {
                            PermissionHelper.requestOverlayPermission(context)
                        }
                    )

                    PermissionCard(
                        title = "Quyền Hỗ trợ tiếp cận (Accessibility)",
                        description = "Phát hiện ứng dụng được mở tức thì trong 15ms.",
                        isGranted = hasAccessibilityPermission,
                        onGrant = {
                            PermissionHelper.requestAccessibilityPermission(context)
                        }
                    )

                    PermissionCard(
                        title = "Quyền Camera ngầm",
                        description = "Chụp ảnh kẻ xâm nhập bằng camera trước khi cố mở app.",
                        isGranted = hasCameraPermission,
                        onGrant = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )

                    PermissionCard(
                        title = "Quyền Quản trị viên (Device Admin)",
                        description = "Ngăn chặn việc gỡ cài đặt LockApp trái phép.",
                        isGranted = hasAdminPermission,
                        onGrant = {
                            PermissionHelper.requestDeviceAdmin(context)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { finishSetup() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hoàn tất & Bắt đầu bảo vệ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quay lại đổi mã PIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                Color(0xFF10B981).copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

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
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                FilledTonalButton(
                    onClick = onGrant,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Cấp quyền", fontSize = 12.sp)
                }
            }
        }
    }
}
