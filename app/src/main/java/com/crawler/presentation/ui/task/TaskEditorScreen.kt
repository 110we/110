package com.crawler.presentation.ui.task

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.domain.model.BodyType
import com.crawler.domain.model.CrawlTask
import com.crawler.domain.model.HttpMethod
import com.crawler.domain.model.JsRenderingConfig
import com.crawler.domain.model.RequestConfig
import com.crawler.domain.model.ScheduleConfig
import com.crawler.domain.model.ScheduleType
import com.crawler.domain.model.SyncConfig
import com.crawler.presentation.viewmodel.TaskViewModel
import com.crawler.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOnEnterAction
import androidx.compose.foundation.text.KeyboardAction
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    taskId: String?,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val taskViewModel: TaskViewModel = viewModel()
    val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.crawler.presentation.viewmodel.SettingsViewModel>()

    var name by remember { mutableStateOf("") }
    var baseUrls by remember { mutableStateOf("") }
    var includePatterns by remember { mutableStateOf("") }
    var excludePatterns by remember { mutableStateOf("") }
    var maxDepth by remember { mutableStateOf(3) }
    var maxPages by remember { mutableStateOf(1000) }

    var httpMethod by remember { mutableStateOf(HttpMethod.GET) }
    var headers by remember { mutableStateOf("{}") }
    var cookies by remember { mutableStateOf("{}") }
    var body by remember { mutableStateOf("") }
    var bodyType by remember { mutableStateOf(BodyType.NONE) }
    var timeoutSeconds by remember { mutableStateOf(30) }
    var followRedirects by remember { mutableStateOf(true) }
    var maxRedirects by remember { mutableStateOf(10) }
    var userAgent by remember { mutableStateOf("") }

    var scheduleEnabled by remember { mutableStateOf(false) }
    var scheduleType by remember { mutableStateOf(ScheduleType.DAILY) }
    var cronExpression by remember { mutableStateOf("") }
    var timeOfDay by remember { mutableStateOf("02:00") }
    var dayOfWeek by remember { mutableStateOf(1) }
    var dayOfMonth by remember { mutableStateOf(1) }

    var jsEnabled by remember { mutableStateOf(false) }
    var waitCondition by remember { mutableStateOf(com.crawler.domain.model.WaitCondition.NETWORK_IDLE) }
    var waitSelector by remember { mutableStateOf("") }
    var waitScript by remember { mutableStateOf("") }
    var jsTimeout by remember { mutableStateOf(30) }

    var syncEnabled by remember { mutableStateOf(false) }
    var syncEndpoint by remember { mutableStateOf("") }
    var syncAuthType by remember { mutableStateOf(com.crawler.domain.model.AuthType.BEARER) }
    var syncUsername by remember { mutableStateOf("") }
    var syncPassword by remember { mutableStateOf("") }
    var syncOnComplete by remember { mutableStateOf(true) }

    val isEditing = taskId != null && taskId != "new"
    val error by taskViewModel.error.collectAsState()

    // 加载现有任务
    androidx.compose.runtime.LaunchedEffect(taskId) {
        if (isEditing) {
            taskViewModel.tasks.value.firstOrNull { it.id == taskId }?.let { task ->
                name = task.name
                baseUrls = task.baseUrls.joinToString("\n")
                includePatterns = task.urlPatterns.includePatterns.joinToString("\n")
                excludePatterns = task.urlPatterns.excludePatterns.joinToString("\n")
                maxDepth = task.urlPatterns.maxDepth
                maxPages = task.urlPatterns.maxPages

                httpMethod = task.requestConfig.method
                headers = kotlinx.serialization.json.Json.encodeToString(task.requestConfig.headers)
                cookies = kotlinx.serialization.json.Json.encodeToString(task.requestConfig.cookies)
                body = task.requestConfig.body ?: ""
                bodyType = task.requestConfig.bodyType
                timeoutSeconds = task.requestConfig.timeoutSeconds
                followRedirects = task.requestConfig.followRedirects
                maxRedirects = task.requestConfig.maxRedirects
                userAgent = task.requestConfig.userAgent ?: ""

                scheduleEnabled = task.scheduleConfig?.enabled == true
                scheduleType = task.scheduleConfig?.type ?: ScheduleType.DAILY
                cronExpression = task.scheduleConfig?.cronExpression ?: ""
                timeOfDay = task.scheduleConfig?.timeOfDay?.toString() ?: "02:00"
                dayOfWeek = task.scheduleConfig?.dayOfWeek?.ordinal ?: 1
                dayOfMonth = task.scheduleConfig?.dayOfMonth ?: 1

                jsEnabled = task.jsRenderingConfig?.enabled == true
                waitCondition = task.jsRenderingConfig?.waitCondition ?: com.crawler.domain.model.WaitCondition.NETWORK_IDLE
                waitSelector = task.jsRenderingConfig?.waitSelector ?: ""
                waitScript = task.jsRenderingConfig?.waitScript ?: ""
                jsTimeout = task.jsRenderingConfig?.timeoutSeconds ?: 30

                syncEnabled = task.syncConfig?.enabled == true
                syncEndpoint = task.syncConfig?.endpoint ?: ""
                syncAuthType = task.syncConfig?.authType ?: com.crawler.domain.model.AuthType.BEARER
                syncUsername = task.syncConfig?.credentials.username ?: ""
                syncPassword = task.syncConfig?.credentials.password ?: ""
                syncOnComplete = task.syncConfig?.syncOnComplete ?: true
            }
        } else {
            // 新建任务时使用默认设置
            settingsViewModel.settings.value?.let { settings ->
                timeoutSeconds = settings.defaultTimeoutSeconds
                maxRedirects = settings.defaultMaxRedirects
                userAgent = settings.defaultUserAgent
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑任务" else "新建任务") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        saveTask()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 错误提示
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
                    // 基本信息
                    item { SectionCard(title = "基本信息") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("任务名称 *") },
                            placeholder = { Text("输入任务名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }}

                    // 基础 URLs
                    item { SectionCard(title = "基础 URLs (每行一个)") {
                        OutlinedTextField(
                            value = baseUrls,
                            onValueChange = { baseUrls = it },
                            label = { Text("https://example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }}

                    // URL 匹配规则
                    item { SectionCard(title = "URL 匹配规则") {
                        OutlinedTextField(
                            value = includePatterns,
                            onValueChange = { includePatterns = it },
                            label = { Text("包含模式 (正则，每行一个)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = excludePatterns,
                            onValueChange = { excludePatterns = it },
                            label = { Text("排除模式 (正则，每行一个)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = maxDepth.toString(),
                                onValueChange = { maxDepth = it.toIntOrNull() ?: 3 },
                                label = { Text("最大深度") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = maxPages.toString(),
                                onValueChange = { maxPages = it.toIntOrNull() ?: 1000 },
                                label = { Text("最大页面数") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }}

                    // 请求配置
                    item { SectionCard(title = "请求配置") {
                        // HTTP 方法
                        DropdownMenuButton(
                            text = httpMethod.name,
                            items = HttpMethod.values().map { it.name },
                            onSelect = { httpMethod = HttpMethod.valueOf(it) }
                        )

                        // Body 类型
                        DropdownMenuButton(
                            text = bodyType.name,
                            items = BodyType.values().map { it.name },
                            onSelect = { bodyType = BodyType.valueOf(it) }
                        )

                        OutlinedTextField(
                            value = headers,
                            onValueChange = { headers = it },
                            label = { Text("请求头 (JSON)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        OutlinedTextField(
                            value = cookies,
                            onValueChange = { cookies = it },
                            label = { Text("Cookies (JSON)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("请求体") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = timeoutSeconds.toString(),
                                onValueChange = { timeoutSeconds = it.toIntOrNull() ?: 30 },
                                label = { Text("超时(秒)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = maxRedirects.toString(),
                                onValueChange = { maxRedirects = it.toIntOrNull() ?: 10 },
                                label = { Text("最大重定向") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                        OutlinedTextField(
                            value = userAgent,
                            onValueChange = { userAgent = it },
                            label = { Text("User-Agent (留空使用默认)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = followRedirects,
                                onCheckedChange = { followRedirects = it },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text("跟随重定向", fontSize = 14.sp)
                        }
                    }}

                    // 调度配置
                    item { SectionCard(title = "定时调度") {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = scheduleEnabled,
                                onCheckedChange = { scheduleEnabled = it },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text("启用定时", fontSize = 14.sp)
                        }

                        if (scheduleEnabled) {
                            DropdownMenuButton(
                                text = scheduleType.name,
                                items = ScheduleType.values().map { it.name },
                                onSelect = { scheduleType = ScheduleType.valueOf(it) }
                            )

                            when (scheduleType) {
                                ScheduleType.CUSTOM -> {
                                    OutlinedTextField(
                                        value = cronExpression,
                                        onValueChange = { cronExpression = it },
                                        label = { Text("Cron 表达式 (如: 0 2 * * *)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                ScheduleType.DAILY, ScheduleType.WEEKLY, ScheduleType.MONTHLY -> {
                                    OutlinedTextField(
                                        value = timeOfDay,
                                        onValueChange = { timeOfDay = it },
                                        label = { Text("运行时间 (HH:mm)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (scheduleType == ScheduleType.WEEKLY) {
                                        DropdownMenuButton(
                                            text = "周${dayOfWeek + 1}",
                                            items = (0..6).map { "周${it + 1}" },
                                            onSelect = { dayOfWeek = it.indexOfFirst { it == "周${dayOfWeek + 1}" } }
                                        )
                                    }
                                    if (scheduleType == ScheduleType.MONTHLY) {
                                        OutlinedTextField(
                                            value = dayOfMonth.toString(),
                                            onValueChange = { dayOfMonth = it.toIntOrNull() ?: 1 },
                                            label = { Text("日期 (1-31)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                        }
                    }}

                    // JS 渲染
                    item { SectionCard(title = "JavaScript 渲染") {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = jsEnabled,
                                onCheckedChange = { jsEnabled = it },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text("启用 JS 渲染", fontSize = 14.sp)
                        }

                        if (jsEnabled) {
                            DropdownMenuButton(
                                text = waitCondition.name,
                                items = com.crawler.domain.model.WaitCondition.values().map { it.name },
                                onSelect = { waitCondition = com.crawler.domain.model.WaitCondition.valueOf(it) }
                            )

                            when (waitCondition) {
                                com.crawler.domain.model.WaitCondition.SELECTOR -> {
                                    OutlinedTextField(
                                        value = waitSelector,
                                        onValueChange = { waitSelector = it },
                                        label = { Text("等待选择器 (CSS/XPath)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                com.crawler.domain.model.WaitCondition.SCRIPT -> {
                                    OutlinedTextField(
                                        value = waitScript,
                                        onValueChange = { waitScript = it },
                                        label = { Text("等待脚本 (返回 true 时继续)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = jsTimeout.toString(),
                                onValueChange = { jsTimeout = it.toIntOrNull() ?: 30 },
                                label = { Text("渲染超时(秒)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }}

                    // 同步配置
                    item { SectionCard(title = "结果同步") {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = syncEnabled,
                                onCheckedChange = { syncEnabled = it },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text("启用同步", fontSize = 14.sp)
                        }

                        if (syncEnabled) {
                            OutlinedTextField(
                                value = syncEndpoint,
                                onValueChange = { syncEndpoint = it },
                                label = { Text("同步端点 URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenuButton(
                                text = syncAuthType.name,
                                items = com.crawler.domain.model.AuthType.values().map { it.name },
                                onSelect = { syncAuthType = com.crawler.domain.model.AuthType.valueOf(it) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = syncUsername,
                                    onValueChange = { syncUsername = it },
                                    label = { Text("用户名/Key") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = syncPassword,
                                    onValueChange = { syncPassword = it },
                                    label = { Text("密码/Token") },
                                    modifier = Modifier.weight(1f),
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                Switch(
                                    checked = syncOnComplete,
                                    onCheckedChange = { syncOnComplete = it },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                Text("爬取完成后同步", fontSize = 14.sp)
                            }
                        }
                    }}
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            content()
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
    val selectedText = text

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = { /* 不直接编辑 */ },
            label = { Text(text) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .fillMaxWidth()
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