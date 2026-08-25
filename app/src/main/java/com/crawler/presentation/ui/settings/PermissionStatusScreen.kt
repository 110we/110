package com.crawler.presentation.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Chip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.presentation.viewmodel.PermissionViewModel
import com.crawler.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOnEnterAction
import androidx.compose.foundation.text.KeyboardAction
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionStatusScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionViewModel: PermissionViewModel = viewModel()

    val permissionsState by permissionViewModel.permissionsState.collectAsState()
    val isChecking by permissionViewModel.isChecking.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限状态") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { permissionViewModel.checkAllPermissions() }, enabled = !isChecking) {
                        if (isChecking) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CrawlerTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CrawlerTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 说明文字
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = CrawlerTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "", tint = CrawlerTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = "权限说明",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrawlerTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "部分高级功能需要通过 ADB 授予特殊权限。点击操作按钮查看授权命令。",
                                fontSize = 12.sp,
                                color = CrawlerTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PermissionItem.allPermissions) { perm ->
                        PermissionCard(
                            permission = perm,
                            status = permissionsState[perm.name] ?: false,
                            onRequest = { permissionViewModel.requestPermission(perm.name) },
                            onOpenSettings = { permissionViewModel.openAppSettings() },
                            onCopyCommand = { permissionViewModel.copyAdbCommand(perm.name) }
                        )
                    }
                }

                // 底部提示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = CrawlerTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ADB 授权命令示例",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "adb shell pm grant com.crawler android.permission.MANAGE_EXTERNAL_STORAGE\nadb shell pm grant com.crawler android.permission.QUERY_ALL_PACKAGES\nadb shell pm grant com.crawler android.permission.PACKAGE_USAGE_STATS\nadb shell appops set com.crawler MANAGE_EXTERNAL_STORAGE allow",
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = CrawlerTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

data class PermissionItem(
    val name: String,
    val displayName: String,
    val description: String,
    val adbCommand: String,
    val isAdbOnly: Boolean,
    val icon: androidx.compose.material.icons.filled.Icon
)

object PermissionItem {
    val allPermissions = listOf(
        PermissionItem(
            name = Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            displayName = "管理外部存储",
            description = "允许应用读写外部存储的所有文件，用于导出爬取结果到下载目录",
            adbCommand = "adb shell pm grant com.crawler android.permission.MANAGE_EXTERNAL_STORAGE\nadb shell appops set com.crawler MANAGE_EXTERNAL_STORAGE allow",
            isAdbOnly = true,
            icon = Icons.Default.Storage
        ),
        PermissionItem(
            name = Manifest.permission.QUERY_ALL_PACKAGES,
            displayName = "查询所有包",
            description = "允许应用查看设备上安装的所有应用包信息，用于应用分析功能",
            adbCommand = "adb shell pm grant com.crawler android.permission.QUERY_ALL_PACKAGES",
            isAdbOnly = true,
            icon = Icons.Default.Apps
        ),
        PermissionItem(
            name = Manifest.permission.PACKAGE_USAGE_STATS,
            displayName = "应用使用统计",
            description = "允许应用访问应用使用统计信息，用于分析应用使用情况",
            adbCommand = "adb shell pm grant com.crawler android.permission.PACKAGE_USAGE_STATS\nadb shell appops set com.crawler PACKAGE_USAGE_STATS allow",
            isAdbOnly = true,
            icon = Icons.Default.Analytics
        ),
        PermissionItem(
            name = Manifest.permission.INTERNET,
            displayName = "网络访问",
            description = "允许应用访问网络，爬取网页内容必需",
            adbCommand = "",
            isAdbOnly = false,
            icon = Icons.Default.Public
        ),
        PermissionItem(
            name = Manifest.permission.ACCESS_NETWORK_STATE,
            displayName = "网络状态",
            description = "允许应用查看网络连接状态",
            adbCommand = "",
            isAdbOnly = false,
            icon = Icons.Default.NetworkCheck
        ),
        PermissionItem(
            name = Manifest.permission.FOREGROUND_SERVICE,
            displayName = "前台服务",
            description = "允许应用运行前台服务，用于后台爬取任务",
            adbCommand = "",
            isAdbOnly = false,
            icon = Icons.Default.PlayCircle
        ),
        PermissionItem(
            name = Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            displayName = "前台服务-数据同步",
            description = "允许数据同步类型的前台服务",
            adbCommand = "",
            isAdbOnly = false,
            icon = Icons.Default.Sync
        ),
        PermissionItem(
            name = Manifest.permission.POST_NOTIFICATIONS,
            displayName = "发送通知",
            description = "允许应用发送通知，用于爬取进度和完成提醒",
            adbCommand = "",
            isAdbOnly = false,
            icon = Icons.Default.Notifications
        )
    )
}

@Composable
fun PermissionCard(
    permission: PermissionItem,
    status: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyCommand: () -> Unit
) {
    val isGranted = status
    val isAdbOnly = permission.isAdbOnly

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isGranted)
                CrawlerTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else
                CrawlerTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 图标 + 状态指示
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        permission.icon,
                        contentDescription = "",
                        tint = if (isGranted) CrawlerTheme.colorScheme.primary else CrawlerTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isGranted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已授权",
                            tint = CrawlerTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        )
                    } else {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "未授权",
                            tint = CrawlerTheme.colorScheme.error,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                // 权限信息
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = permission.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Chip(
                            onClick = { /* noop */ },
                            colors = androidx.compose.material3.ChipDefaults.chipColors(
                                containerColor = if (isGranted)
                                    CrawlerTheme.colorScheme.primaryContainer
                                else
                                    CrawlerTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = if (isGranted) "已授权" else "未授权",
                                fontSize = 12.sp,
                                color = if (isGranted) CrawlerTheme.colorScheme.onPrimaryContainer else CrawlerTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Text(
                        text = permission.description,
                        fontSize = 13.sp,
                        color = CrawlerTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdbOnly) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "", tint = CrawlerTheme.colorScheme.warning, size = 14.sp)
                            Text(
                                text = "需要 ADB 授权",
                                fontSize = 12.sp,
                                color = CrawlerTheme.colorScheme.warning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 操作按钮
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isGranted && isAdbOnly) {
                    // ADB 权限：显示复制命令、打开设置、请求授权
                    Button(onClick = onCopyCommand) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        Text("复制 ADB 命令")
                    }
                    Button(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                        Text("打开应用设置")
                    }
                    Button(
                        onClick = onRequest,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = CrawlerTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "请求")
                        Text("请求授权")
                    }
                } else if (!isGranted && !isAdbOnly) {
                    // 普通权限：请求授权、打开设置
                    Button(
                        onClick = onRequest,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = CrawlerTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "请求")
                        Text("请求授权")
                    }
                    Button(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                        Text("打开应用设置")
                    }
                } else {
                    // 已授权：显示撤销、打开设置
                    Button(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                        Text("查看设置")
                    }
                    Button(onClick = { /* 撤销权限需要去系统设置 */ }) {
                        Icon(Icons.Default.Info, contentDescription = "信息")
                        Text("在系统设置中撤销")
                    }
                }
            }
        }
    }
}