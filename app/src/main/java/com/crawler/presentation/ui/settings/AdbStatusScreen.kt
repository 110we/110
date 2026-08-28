package com.crawler.presentation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Chip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.presentation.viewmodel.AdbViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbStatusScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adbViewModel: AdbViewModel = viewModel()
    val state by adbViewModel.uiState.collectAsState()
    var testCommand by remember { mutableStateOf("id") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB 状态") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { adbViewModel.refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdbOverviewCard(
                        state = state,
                        onRequest = { adbViewModel.requestShizukuPermission() },
                        onOpenShizuku = { adbViewModel.openShizukuApp() }
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "模式检测",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                            ModeStatusRow(label = "Shizuku 已安装", active = state.shizukuInstalled)
                            ModeStatusRow(label = "Shizuku 服务", active = state.shizukuAvailable)
                            ModeStatusRow(label = "Shizuku 已授权", active = state.shizukuAuthorized)
                            ModeStatusRow(label = "Root 可用", active = state.rootAvailable)
                            ModeStatusRow(label = "当前模式", active = true, extra = state.activeMode)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "命令测试",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = testCommand,
                                    onValueChange = { testCommand = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("shell 命令") }
                                )
                                Button(
                                    onClick = { adbViewModel.runTestCommand(testCommand) },
                                    enabled = !state.busy
                                ) {
                                    if (state.busy) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "执行")
                                        Text("执行")
                                    }
                                }
                            }
                            state.lastCommand?.let { command ->
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "执行: $command",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            state.lastOutput?.let { output ->
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = output,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "使用说明",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Shizuku 通过 ADB 或 Root 激活后可获得 shell 权限，用于访问受保护的数据目录（如应用私有数据、系统数据库等）。\n\n激活步骤：\n1. 安装并打开 Shizuku 应用\n2. 在 Shizuku 中通过 ADB（无线调试）或 Root 启动服务\n3. 回到本页点击\"授权 Shizuku\"\n4. 在系统弹窗中允许本应用使用 Shizuku",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdbOverviewCard(
    state: AdbViewModel.AdbUiState,
    onRequest: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val ready = state.shizukuAuthorized || state.rootAvailable

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (ready)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "",
                        tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Icon(
                        if (ready) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = "",
                        tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ADB 能力",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (ready) "当前可用，模式：${state.activeMode}" else "当前不可用",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Chip(onClick = { /* noop */ }) {
                    Text(
                        text = if (ready) "已就绪" else "未就绪",
                        fontSize = 12.sp,
                        color = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            when {
                !state.shizukuInstalled -> {
                    OutlinedButton(onClick = { /* 提示安装 */ }) {
                        Text("未检测到 Shizuku，请先安装")
                    }
                }
                !state.shizukuAvailable -> {
                    OutlinedButton(onClick = onOpenShizuku) {
                        Text("打开 Shizuku 并启动服务")
                    }
                }
                !state.shizukuAuthorized -> {
                    Button(
                        onClick = onRequest,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "授权")
                        Text("授权 Shizuku")
                    }
                }
                else -> {
                    OutlinedButton(onClick = { /* 已授权 */ }) {
                        Text("已授权")
                    }
                }
            }
        }
    }
}

@Composable
fun ModeStatusRow(
    label: String,
    active: Boolean,
    extra: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (extra.isNotBlank()) {
                Text(
                    text = extra,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (active) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (active) "是" else "否",
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
