package com.lockapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.lockapp.data.AppDatabase
import com.lockapp.data.entity.IntruderLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderLogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val logList by database.intruderLogDao().getAllLogs().collectAsState(initial = emptyList())
    var selectedLogForDetail by remember { mutableStateOf<IntruderLog?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    fun deleteSingleLog(log: IntruderLog) {
        scope.launch(Dispatchers.IO) {
            log.photoPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
            database.intruderLogDao().delete(log)
        }
    }

    fun deleteAllLogs() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "intruders")
                if (dir.exists()) {
                    dir.listFiles()?.forEach { it.delete() }
                }
            } catch (_: Exception) {}
            database.intruderLogDao().deleteAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nhật ký xâm nhập",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Tổng số: ${logList.size} lần ghi nhận",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    if (logList.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllConfirmDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Xóa tất cả",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (logList.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Chưa phát hiện xâm nhập nào",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Khi có ai đó cố mở ứng dụng bị khóa hoặc nhập sai mã PIN, camera trước sẽ tự động chụp ảnh và lưu lại tại đây.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logList, key = { it.id }) { log ->
                        IntruderLogCard(
                            log = log,
                            onClick = { selectedLogForDetail = log },
                            onDelete = { deleteSingleLog(log) }
                        )
                    }
                }
            }
        }
    }

    // === DIALOG PHÓNG TO ẢNH VÀ XEM CHI TIẾT ===
    selectedLogForDetail?.let { log ->
        IntruderDetailDialog(
            log = log,
            onDismiss = { selectedLogForDetail = null },
            onDelete = {
                deleteSingleLog(log)
                selectedLogForDetail = null
            }
        )
    }

    // === DIALOG XÁC NHẬN XÓA TẤT CẢ ===
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("Xóa toàn bộ nhật ký?") },
            text = { Text("Thao tác này sẽ xóa vĩnh viễn tất cả hình ảnh chụp kẻ xâm nhập và lịch sử bảo vệ.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteAllLogs()
                        showDeleteAllConfirmDialog = false
                    }
                ) {
                    Text("Xóa tất cả", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun IntruderLogCard(
    log: IntruderLog,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (!log.photoPath.isNullOrBlank() && File(log.photoPath).exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = File(log.photoPath)),
                        contentDescription = "Ảnh kẻ xâm nhập",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.NoPhotography,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Thông tin
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.targetAppName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formattedTime = remember(log.timestamp) {
                    SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault()).format(Date(log.timestamp))
                }
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = when {
                    log.wrongAttempts > 0 -> "Nhập sai PIN: ${log.wrongAttempts} lần"
                    !log.note.isNullOrBlank() -> log.note
                    else -> "Cố gắng mở app bị khóa"
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (log.wrongAttempts > 0) MaterialTheme.colorScheme.error else Color(0xFFF59E0B)
                )
            }

            // Nút xóa
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa bản ghi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun IntruderDetailDialog(
    log: IntruderLog,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chi tiết xâm nhập",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Ảnh chụp lớn
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!log.photoPath.isNullOrBlank() && File(log.photoPath).exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = File(log.photoPath)),
                            contentDescription = "Ảnh chụp",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NoPhotography,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Không có ảnh chụp", color = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thông tin chi tiết
                DetailRow(label = "Ứng dụng:", value = log.targetAppName)
                DetailRow(label = "Package:", value = log.targetAppPackage)
                val timeStr = SimpleDateFormat("HH:mm:ss, dd/MM/yyyy", Locale.getDefault()).format(Date(log.timestamp))
                DetailRow(label = "Thời gian:", value = timeStr)
                if (log.wrongAttempts > 0) {
                    DetailRow(label = "Số lần sai PIN:", value = "${log.wrongAttempts} lần")
                }
                if (!log.note.isNullOrBlank()) {
                    DetailRow(label = "Ghi chú:", value = log.note)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Xóa bản ghi", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = onDismiss) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
