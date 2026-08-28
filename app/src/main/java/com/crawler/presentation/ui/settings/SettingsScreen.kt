package com.crawler.presentation.ui.settings

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.presentation.viewmodel.SettingsViewModel
import com.crawler.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPermissionsClick: () -> Unit,
    onAdbStatusClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()

    val settings by settingsViewModel.settings.collectAsState()

    var defaultTimeout by remember { mutableStateOf(settings?.defaultTimeoutSeconds ?: 30) }
    var defaultMaxRedirects by remember { mutableStateOf(settings?.defaultMaxRedirects ?: 10) }
    var defaultUserAgent by remember { mutableStateOf(settings?.defaultUserAgent ?: "") }
    var defaultConcurrency by remember { mutableStateOf(settings?.defaultConcurrency ?: 4) }
    var defaultRateLimit by remember { mutableStateOf(1000) }

    var autoBackup by remember { mutableStateOf(false) }
    var backupInterval by remember { mutableStateOf(24) }
    var maxBackupFiles by remember { mutableStateOf(10) }

    var themeMode by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf("zh") }
    var showNotifications by remember { mutableStateOf(true) }
    var notificationSound by remember { mutableStateOf(true) }

    var logLevel by remember { mutableStateOf("INFO") }
    var maxLogFiles by remember { mutableStateOf(5) }
    var maxLogSizeMb by remember { mutableStateOf(10) }

    var clearCacheOnExit by remember { mutableStateOf(false) }
    var cacheMaxSizeMb by remember { mutableStateOf(500) }

    val error by settingsViewModel.error.collectAsState()
    val isSaving by settingsViewModel.isSaving.collectAsState()

    fun saveSettings() {
        val newSettings = com.crawler.data.entity.AppSettingsEntity(
            id = 1,
            defaultTimeoutSeconds = defaultTimeout,
            defaultMaxRedirects = defaultMaxRedirects,
            defaultUserAgent = defaultUserAgent,
            defaultConcurrency = defaultConcurrency,
            globalRateLimitPerSecond = defaultRateLimit.toDouble(),
            robotsTxtCompliance = true,
            jsRenderingDefaultEnabled = false,
            jsRenderingDefaultTimeout = 30
        )
        settingsViewModel.saveSettings(newSettings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { saveSettings() },
                        enabled = !isSaving,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSaving) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                            Text("保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                error?.let { err ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 爬取默认设置
                    item { SettingsSection(title = "爬取默认设置", icon = Icons.Default.Settings) {
                        SettingRow(
                            label = "默认超时 (秒)",
                            content = {
                                OutlinedTextField(
                                    value = defaultTimeout.toString(),
                                    onValueChange = { defaultTimeout = it.toIntOrNull() ?: 30 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                        SettingRow(
                            label = "默认最大重定向",
                            content = {
                                OutlinedTextField(
                                    value = defaultMaxRedirects.toString(),
                                    onValueChange = { defaultMaxRedirects = it.toIntOrNull() ?: 10 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                        SettingRow(
                            label = "默认 User-Agent",
                            content = {
                                OutlinedTextField(
                                    value = defaultUserAgent,
                                    onValueChange = { defaultUserAgent = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("留空使用系统默认") }
                                )
                            }
                        )
                        SettingRow(
                            label = "默认并发数",
                            content = {
                                Slider(
                                    value = defaultConcurrency.toFloat(),
                                    onValueChange = { defaultConcurrency = it.roundToInt() },
                                    valueRange = 1f..16f,
                                    steps = 15,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                                Text("${defaultConcurrency}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            }
                        )
                        SettingRow(
                            label = "默认速率限制 (ms)",
                            content = {
                                OutlinedTextField(
                                    value = defaultRateLimit.toString(),
                                    onValueChange = { defaultRateLimit = it.toIntOrNull() ?: 1000 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                    }}

                    // 备份设置
                    item { SettingsSection(title = "自动备份", icon = Icons.Default.Backup) {
                        SettingRow(
                            label = "启用自动备份",
                            content = {
                                Switch(
                                    checked = autoBackup,
                                    onCheckedChange = { autoBackup = it }
                                )
                            }
                        )
                        SettingRow(
                            label = "备份间隔 (小时)",
                            content = {
                                OutlinedTextField(
                                    value = backupInterval.toString(),
                                    onValueChange = { backupInterval = it.toIntOrNull() ?: 24 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                        SettingRow(
                            label = "最大保留备份数",
                            content = {
                                OutlinedTextField(
                                    value = maxBackupFiles.toString(),
                                    onValueChange = { maxBackupFiles = it.toIntOrNull() ?: 10 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                    }}

                    // 界面设置
                    item { SettingsSection(title = "界面与通知", icon = Icons.Default.Palette) {
                        SettingRow(
                            label = "主题模式",
                            content = {
                                DropdownMenuButton(
                                    text = when (themeMode) { 0 -> "跟随系统"; 1 -> "浅色"; 2 -> "深色"; else -> "跟随系统" },
                                    items = listOf("跟随系统", "浅色", "深色"),
                                    onSelect = { themeMode = listOf("跟随系统", "浅色", "深色").indexOf(it) }
                                )
                            }
                        )
                        SettingRow(
                            label = "语言",
                            content = {
                                DropdownMenuButton(
                                    text = when (language) { "zh" -> "中文"; "en" -> "English"; else -> "中文" },
                                    items = listOf("中文", "English"),
                                    onSelect = { language = if (it == "中文") "zh" else "en" }
                                )
                            }
                        )
                        SettingRow(
                            label = "显示通知",
                            content = {
                                Switch(
                                    checked = showNotifications,
                                    onCheckedChange = { showNotifications = it }
                                )
                            }
                        )
                        SettingRow(
                            label = "通知声音",
                            content = {
                                Switch(
                                    checked = notificationSound,
                                    onCheckedChange = { notificationSound = it }
                                )
                            }
                        )
                    }}

                    // 日志设置
                    item { SettingsSection(title = "日志设置", icon = Icons.Default.Description) {
                        SettingRow(
                            label = "日志级别",
                            content = {
                                DropdownMenuButton(
                                    text = logLevel,
                                    items = listOf("DEBUG", "INFO", "WARN", "ERROR"),
                                    onSelect = { logLevel = it }
                                )
                            }
                        )
                        SettingRow(
                            label = "最大日志文件数",
                            content = {
                                OutlinedTextField(
                                    value = maxLogFiles.toString(),
                                    onValueChange = { maxLogFiles = it.toIntOrNull() ?: 5 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                        SettingRow(
                            label = "单个日志最大 (MB)",
                            content = {
                                OutlinedTextField(
                                    value = maxLogSizeMb.toString(),
                                    onValueChange = { maxLogSizeMb = it.toIntOrNull() ?: 10 },
                                    modifier = Modifier.width(120.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        )
                    }}

                    // 缓存设置
                    item { SettingsSection(title = "缓存设置", icon = Icons.Default.DeleteSweep) {
                        SettingRow(
                            label = "退出时清理缓存",
                            content = {
                                Switch(
                                    checked = clearCacheOnExit,
                                    onCheckedChange = { clearCacheOnExit = it }
                                )
                            }
                        )
                        SettingRow(
                            label = "最大缓存大小 (MB)",
                            content = {
                                Slider(
                                    value = cacheMaxSizeMb.toFloat(),
                                    onValueChange = { cacheMaxSizeMb = it.roundToInt() },
                                    valueRange = 50f..2000f,
                                    steps = 39,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                                Text("${cacheMaxSizeMb}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                            }
                        )
                    }}

                    // 权限管理
                    item { SettingsSection(title = "权限管理", icon = Icons.Default.Security) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "查看和管理应用权限",
                                fontSize = 14.sp
                            )
                            Button(onClick = onPermissionsClick) {
                                Text("权限状态")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ADB 能力（Shizuku / Root）",
                                fontSize = 14.sp
                            )
                            Button(onClick = onAdbStatusClick) {
                                Text("ADB 状态")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "任务执行历史记录",
                                fontSize = 14.sp
                            )
                            Button(onClick = onHistoryClick) {
                                Text("执行历史")
                            }
                        }
                    }}

                    // 危险操作区
                    item { SettingsSection(title = "危险操作", icon = Icons.Default.Warning, isDangerous = true) {
                        DangerButton(
                            text = "清除所有缓存",
                            description = "删除所有临时文件和缓存数据",
                            onClick = { settingsViewModel.clearCache() }
                        )
                        DangerButton(
                            text = "重置所有设置",
                            description = "恢复所有设置为默认值",
                            onClick = { settingsViewModel.resetToDefaults() }
                        )
                        DangerButton(
                            text = "导出所有数据",
                            description = "导出所有任务、规则、结果为备份文件",
                            onClick = { settingsViewModel.exportAllData() }
                        )
                        DangerButton(
                            text = "删除所有数据",
                            description = "永久删除所有任务、规则、结果和设置",
                            onClick = { settingsViewModel.deleteAllData() },
                            isDestructive = true
                        )
                    }}
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    isDangerous: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isDangerous)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = "", tint = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
        content()
    }
}

@Composable
fun DangerButton(
    text: String,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isDestructive)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("执行", color = if (isDestructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun DropdownMenuButton(
    text: String,
    items: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = text,
            onValueChange = { /* 不直接编辑 */ },
            label = { Text("") },
            modifier = Modifier
                .width(200.dp)
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = "展开")

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}