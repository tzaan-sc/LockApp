@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lockapp.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockapp.camera.IntruderCaptureManager
import com.lockapp.data.AppDatabase
import com.lockapp.service.LockAccessibilityService
import com.lockapp.service.LockService
import com.lockapp.ui.theme.*
import com.lockapp.util.PermissionHelper
import com.lockapp.util.RepairModeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Màn hình chính (Home) của LockApp.
 * Hiển thị trạng thái bảo vệ, nút Repair Mode, và quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAppList: () -> Unit = {},
    onNavigateToIntruderLog: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val repairModeManager = remember { RepairModeManager(context) }
    val scope = rememberCoroutineScope()

    var isRepairMode by remember { mutableStateOf(repairModeManager.isRepairModeActive()) }
    var showDeactivatePinDialog by remember { mutableStateOf(false) }
    var deactivatePinInput by remember { mutableStateOf("") }
    var deactivatePinError by remember { mutableStateOf<String?>(null) }

    val lockedCount by database.lockedAppDao().getLockedCount().collectAsState(initial = 0)
    val intruderCount by database.intruderLogDao().getTotalCount().collectAsState(initial = 0)
    val scrollState = rememberScrollState()

    // Animation cho nút Repair Mode
    val switchScale by animateFloatAsState(
        targetValue = if (isRepairMode) 1.05f else 1f,
        animationSpec = tween(300),
        label = "switchScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LockApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Cài đặt"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // === REPAIR MODE CARD ===
            RepairModeCard(
                isActive = isRepairMode,
                scale = switchScale,
                onToggle = {
                    if (!isRepairMode) {
                        isRepairMode = true
                        repairModeManager.activateRepairMode()
                        startLockService(context)
                        scope.launch(Dispatchers.IO) {
                            database.lockedAppDao().lockAllRepairModeDefaults()
                        }
                    } else {
                        // Yêu cầu nhập PIN trước khi tắt
                        deactivatePinInput = ""
                        deactivatePinError = null
                        showDeactivatePinDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // === STATUS ROW ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Shield,
                    value = lockedCount.toString(),
                    label = "App bảo vệ",
                    color = StatusProtected
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CameraFront,
                    value = intruderCount.toString(),
                    label = "Lần xâm nhập",
                    color = AccentRed
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === QUICK ACTIONS ===
            Text(
                "Hành động nhanh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            QuickActionItem(
                icon = Icons.Outlined.Apps,
                title = "Quản lý ứng dụng khóa",
                subtitle = "Chọn ứng dụng cần bảo vệ",
                onClick = onNavigateToAppList
            )

            QuickActionItem(
                icon = Icons.Outlined.PhotoCamera,
                title = "Nhật ký xâm nhập",
                subtitle = "Xem ảnh và lịch sử truy cập trái phép",
                onClick = onNavigateToIntruderLog
            )

            QuickActionItem(
                icon = Icons.Outlined.Security,
                title = "Trạng thái quyền",
                subtitle = buildPermissionStatus(context),
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(20.dp))

            // === USB WARNING ===
            if (PermissionHelper.isUsbDebuggingEnabled(context)) {
                UsbWarningCard()
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (showDeactivatePinDialog) {
            AlertDialog(
                onDismissRequest = { showDeactivatePinDialog = false },
                title = { Text("Tắt Chế Độ Sửa Máy", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Vui lòng nhập mã PIN bảo mật để tắt chế độ bảo vệ:",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = deactivatePinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    deactivatePinInput = it
                                }
                            },
                            label = { Text("Mã PIN") },
                            singleLine = true,
                            isError = deactivatePinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (deactivatePinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = deactivatePinError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (repairModeManager.verifyPin(deactivatePinInput)) {
                                repairModeManager.deactivateRepairMode()
                                isRepairMode = false
                                showDeactivatePinDialog = false
                            } else {
                                deactivatePinError = "Mã PIN không đúng"
                                IntruderCaptureManager.getInstance(context).vibrateOnError()
                                IntruderCaptureManager.getInstance(context).captureIntruder(
                                    targetAppPackage = "com.lockapp",
                                    targetAppName = "LockApp",
                                    wrongAttempts = 1,
                                    note = "Cố gắng tắt Repair Mode sai PIN"
                                )
                            }
                        }
                    ) {
                        Text("Xác nhận")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivatePinDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Composable
private fun RepairModeCard(
    isActive: Boolean,
    scale: Float,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) RepairModeActive else DarkSurfaceVariant,
        animationSpec = tween(500),
        label = "bgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isActive) listOf(AccentRed, AccentRed.copy(alpha = 0.7f))
                            else listOf(Primary, PrimaryLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isActive) Icons.Filled.ShieldMoon else Icons.Filled.Shield,
                    contentDescription = null,
                    tint = TextOnPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chế Độ Sửa Máy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = if (isActive) "Đang BẬT bảo vệ" else "Đã TẮT bảo vệ",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Switch
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextOnPrimary,
                    checkedTrackColor = AccentGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurface,
                )
            )
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UsbWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StatusWarning.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = StatusWarning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "⚠️ USB Debugging đang BẬT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = StatusWarning
                )
                Text(
                    "Hãy tắt trước khi đưa máy cho thợ sửa!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun buildPermissionStatus(context: Context): String {
    val checks = listOf(
        "Overlay" to PermissionHelper.canDrawOverlays(context),
        "Trợ năng" to PermissionHelper.isAccessibilityServiceEnabled(context),
        "Camera" to PermissionHelper.hasCameraPermission(context),
    )
    val granted = checks.count { it.second }
    return "$granted/${checks.size} quyền đã cấp"
}

private fun startLockService(context: Context) {
    val intent = Intent(context, LockService::class.java).apply {
        action = LockService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
