package com.lockapp.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.lockapp.data.AppDatabase
import com.lockapp.data.entity.LockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isLocked: Boolean,
    val category: String,
    val isRepairModeDefault: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var appList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }

    val tabs = listOf("Tất cả", "Đang khóa", "Mạng xã hội", "Ảnh & Video", "Tài chính", "Hệ thống")

    // Tải và đồng bộ danh sách ứng dụng
    fun loadApps() {
        scope.launch {
            isLoading = true
            val apps = withContext(Dispatchers.IO) {
                scanInstalledApps(context, database)
            }
            appList = apps
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    // Toggle trạng thái khóa của 1 app
    fun toggleLock(app: InstalledAppItem) {
        val newLockedState = !app.isLocked
        appList = appList.map {
            if (it.packageName == app.packageName) it.copy(isLocked = newLockedState) else it
        }

        scope.launch(Dispatchers.IO) {
            val entity = LockedApp(
                packageName = app.packageName,
                appName = app.appName,
                iconUri = null,
                isLocked = newLockedState,
                category = app.category,
                isRepairModeDefault = app.isRepairModeDefault
            )
            database.lockedAppDao().insert(entity)
        }
    }

    // Khóa nhanh tất cả app đề xuất
    fun lockAllRecommended() {
        scope.launch(Dispatchers.IO) {
            val sensitiveDefaults = setOf(
                "com.android.settings",
                "com.zing.zalo",
                "com.facebook.orca",
                "com.facebook.katana",
                "com.google.android.apps.photos",
                "com.sec.android.gallery3d",
                "com.android.gallery3d",
                "com.android.vending",
                "com.google.android.gm",
                "com.android.mms",
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging"
            )
            val entities = appList.filter { sensitiveDefaults.contains(it.packageName) || it.isRepairModeDefault }.map {
                LockedApp(
                    packageName = it.packageName,
                    appName = it.appName,
                    iconUri = null,
                    isLocked = true,
                    category = it.category,
                    isRepairModeDefault = true
                )
            }
            if (entities.isNotEmpty()) {
                database.lockedAppDao().insertAll(entities)
            }
            database.lockedAppDao().lockAllRepairModeDefaults()
            loadApps()
        }
    }

    // Lọc danh sách app theo tìm kiếm và tab
    val filteredApps = remember(appList, searchQuery, selectedTab) {
        appList.filter { item ->
            val matchSearch = searchQuery.isBlank() ||
                    item.appName.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)

            val matchTab = when (selectedTab) {
                0 -> true // Tất cả
                1 -> item.isLocked // Đang khóa
                2 -> item.category == "SOCIAL"
                3 -> item.category == "MEDIA"
                4 -> item.category == "FINANCE"
                5 -> item.category == "SYSTEM"
                else -> true
            }

            matchSearch && matchTab
        }
    }

    val lockedCount = appList.count { it.isLocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Ứng dụng cần bảo vệ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Đang khóa: $lockedCount/${appList.size} ứng dụng",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
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
                    TextButton(onClick = { lockAllRecommended() }) {
                        Text(
                            "Khóa đề xuất",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
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
        ) {
            // === SEARCH BAR ===
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm ứng dụng...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // === TABS PHÂN LOẠI ===
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === LIST APP ===
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank()) "Không tìm thấy ứng dụng nào phù hợp"
                        else "Chưa có ứng dụng nào trong mục này",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppListItemRow(
                            app = app,
                            onToggle = { toggleLock(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItemRow(
    app: InstalledAppItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon app
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = app.icon),
                        contentDescription = app.appName,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (app.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Tên app & package
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isRepairModeDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Nhạy cảm",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch / Checkbox
            Switch(
                checked = app.isLocked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

/**
 * Quét toàn bộ app đã cài đặt trên thiết bị và đồng bộ trạng thái khóa từ Room DB.
 */
private suspend fun scanInstalledApps(
    context: Context,
    database: AppDatabase
): List<InstalledAppItem> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val launchableActivities = pm.queryIntentActivities(mainIntent, 0)
    val packageSet = mutableSetOf<String>()

    val launchablePackages = launchableActivities.mapNotNull { it.activityInfo?.packageName }
    packageSet.addAll(launchablePackages)
    // Luôn đưa Settings vào danh sách có thể khóa
    packageSet.add("com.android.settings")

    // Lấy toàn bộ danh sách app đã lưu cấu hình trong DB (bao gồm cả đang khóa và đã mở khóa)
    val dbApps = database.lockedAppDao().getAllConfiguredAppsList().associateBy { it.packageName }

    val sensitiveDefaults = setOf(
        "com.android.settings",
        "com.zing.zalo",
        "com.facebook.orca",
        "com.facebook.katana",
        "com.google.android.apps.photos",
        "com.sec.android.gallery3d",
        "com.android.gallery3d",
        "com.android.vending",
        "com.google.android.gm",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    val items = mutableListOf<InstalledAppItem>()

    for (pkg in packageSet) {
        if (pkg == context.packageName) continue // Bỏ qua LockApp

        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val name = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            val isSensitive = sensitiveDefaults.contains(pkg)
            val isLocked = if (dbApps.containsKey(pkg)) {
                dbApps[pkg]?.isLocked == true
            } else {
                isSensitive
            }
            val category = categorizeApp(pkg, appInfo)

            items.add(
                InstalledAppItem(
                    packageName = pkg,
                    appName = name,
                    icon = icon,
                    isLocked = isLocked,
                    category = category,
                    isRepairModeDefault = isSensitive
                )
            )
        } catch (_: Exception) {
        }
    }

    // Sắp xếp: App nhạy cảm và app đang khóa lên trên, sau đó theo tên A-Z
    return items.sortedWith(
        compareByDescending<InstalledAppItem> { it.isLocked }
            .thenByDescending { it.isRepairModeDefault }
            .thenBy { it.appName.lowercase() }
    )
}

private fun categorizeApp(packageName: String, appInfo: ApplicationInfo): String {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("zalo") || pkg.contains("facebook") || pkg.contains("katana") ||
                pkg.contains("instagram") || pkg.contains("tiktok") || pkg.contains("twitter") -> "SOCIAL"

        pkg.contains("orca") || pkg.contains("msg") || pkg.contains("mms") ||
                pkg.contains("telegram") || pkg.contains("viber") || pkg.contains("whatsapp") ||
                pkg.contains("messaging") -> "SOCIAL"

        pkg.contains("gallery") || pkg.contains("photo") || pkg.contains("camera") ||
                pkg.contains("youtube") || pkg.contains("video") -> "MEDIA"

        pkg.contains("bank") || pkg.contains("momo") || pkg.contains("zalopay") ||
                pkg.contains("viettelpay") || pkg.contains("vnpay") || pkg.contains("pay") ||
                pkg.contains("wallet") -> "FINANCE"

        pkg.contains("settings") || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 -> "SYSTEM"

        else -> "OTHER"
    }
}
