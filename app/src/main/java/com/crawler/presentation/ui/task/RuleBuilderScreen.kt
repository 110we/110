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
import com.crawler.domain.model.ExtractionRule
import com.crawler.domain.model.ExtractionStrategy
import com.crawler.domain.model.PostProcessor
import com.crawler.domain.model.PostProcessorType
import com.crawler.domain.model.SelectorType
import com.crawler.presentation.viewmodel.TaskViewModel
import com.crawler.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOnEnterAction
import androidx.compose.foundation.text.KeyboardAction
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    taskId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val taskViewModel: TaskViewModel = viewModel()

    val task = taskViewModel.tasks.value.firstOrNull { it.id == taskId }
    val rules = remember { mutableStateListOf<ExtractionRule>() }

    // 从任务加载现有规则
    androidx.compose.runtime.LaunchedEffect(task) {
        task?.extractionRules?.forEach { rules.add(it) }
    }

    var ruleName by remember { mutableStateOf("") }
    var selectorType by remember { mutableStateOf(SelectorType.CSS) }
    var selector by remember { mutableStateOf("") }
    var attribute by remember { mutableStateOf("text") }
    var strategy by remember { mutableStateOf(ExtractionStrategy.FIRST) }
    var joinSeparator by remember { mutableStateOf(", ") }
    var postProcessors by remember { mutableStateListOf<PostProcessor>() }
    var editingIndex = remember { mutableStateOf<Int?>(null) }

    // 预览状态
    var previewHtml by remember { mutableStateOf("") }
    var previewResult by remember { mutableStateOf<String?>(null) }
    var isPreviewing by remember { mutableStateOf(false) }

    val error by taskViewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提取规则编辑器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        saveRules()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
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
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 左侧：规则列表
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 添加/编辑规则表单
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = CrawlerTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = editingIndex.value != null ? "编辑规则" : "新建规则",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrawlerTheme.colorScheme.primary
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = ruleName,
                                onValueChange = { ruleName = it },
                                label = { Text("规则名称 *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DropdownMenuButton(
                                    text = selectorType.name,
                                    items = SelectorType.values().map { it.name },
                                    onSelect = { selectorType = SelectorType.valueOf(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = attribute,
                                    onValueChange = { attribute = it },
                                    label = { Text("属性 (text/html/attr)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = selector,
                                onValueChange = { selector = it },
                                label = { Text("选择器 *") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 1,
                                maxLines = 3
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DropdownMenuButton(
                                    text = strategy.name,
                                    items = ExtractionStrategy.values().map { it.name },
                                    onSelect = { strategy = ExtractionStrategy.valueOf(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (strategy == ExtractionStrategy.JOIN) {
                                    OutlinedTextField(
                                        value = joinSeparator,
                                        onValueChange = { joinSeparator = it },
                                        label = { Text("连接符") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            // 后处理器
                            PostProcessorSection(postProcessors = postProcessors)

                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(onClick = { cancelEdit() }) {
                                    Text("取消")
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { saveRule() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = CrawlerTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(if (editingIndex.value != null) "更新" else "添加")
                                }
                            }
                        }
                    }

                    // 规则列表
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, true),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = CrawlerTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "规则列表 (${rules.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrawlerTheme.colorScheme.primary
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            if (rules.isEmpty()) {
                                Text(
                                    text = "暂无规则，点击上方添加",
                                    color = CrawlerTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(rules) { rule ->
                                        RuleListItem(
                                            rule = rule,
                                            onEdit = { idx ->
                                                startEdit(idx)
                                            },
                                            onDelete = { idx ->
                                                rules.removeAt(idx)
                                            },
                                            onPreview = { r ->
                                                previewRule(r)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 右侧：预览面板
                Column(
                    modifier = Modifier
                        .width(400.dp)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, true),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = CrawlerTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "实时预览",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrawlerTheme.colorScheme.primary
                                )
                                if (previewHtml.isNotBlank()) {
                                    Button(onClick = { runPreview() }, enabled = !isPreviewing) {
                                        if (isPreviewing) {
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "预览")
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                                            Text("预览")
                                        }
                                    }
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = previewHtml,
                                onValueChange = { previewHtml = it },
                                label = { Text("测试 HTML (粘贴页面片段)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 10,
                                maxLines = 15
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

                            if (previewResult != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = CrawlerTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "预览结果",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = { copyToClipboard(previewResult!!) }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                            }
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = previewResult!!,
                                            fontSize = 13.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else if (previewHtml.isBlank()) {
                                Text(
                                    text = "请输入测试 HTML 并点击预览",
                                    color = CrawlerTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveRule() {
        if (ruleName.isBlank() || selector.isBlank()) return

        val newRule = ExtractionRule(
            name = ruleName,
            selectorType = selectorType,
            selector = selector,
            attribute = attribute,
            strategy = strategy,
            joinSeparator = if (strategy == ExtractionStrategy.JOIN) joinSeparator else null,
            postProcessors = postProcessors.toList()
        )

        if (editingIndex.value != null) {
            rules[editingIndex.value!!] = newRule
            editingIndex.value = null
        } else {
            rules.add(newRule)
        }
        cancelEdit()
    }

    fun cancelEdit() {
        ruleName = ""
        selector = ""
        attribute = "text"
        strategy = ExtractionStrategy.FIRST
        joinSeparator = ", "
        postProcessors.clear()
        editingIndex.value = null
    }

    fun startEdit(index: Int) {
        val rule = rules[index]
        ruleName = rule.name
        selectorType = rule.selectorType
        selector = rule.selector
        attribute = rule.attribute
        strategy = rule.strategy
        joinSeparator = rule.joinSeparator ?: ", "
        postProcessors.clear()
        postProcessors.addAll(rule.postProcessors)
        editingIndex.value = index
    }

    fun saveRules() {
        task?.let { t ->
            val updatedTask = t.copy(extractionRules = rules.toList())
            taskViewModel.updateTask(updatedTask)
            onBack()
        }
    }

    fun previewRule(rule: ExtractionRule) {
        if (previewHtml.isBlank()) return
        isPreviewing = true
        // 这里应该调用 ExtractionEngine 进行预览
        // 简化版：直接在 UI 线程运行
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val engine = com.crawler.domain.engine.ExtractionEngineImpl()
            val result = engine.extract(previewHtml, listOf(rule))
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                previewResult = when (rule.strategy) {
                    ExtractionStrategy.FIRST -> result[rule.name]?.firstOrNull() ?: "无匹配"
                    ExtractionStrategy.ALL_ARRAY -> result[rule.name]?.joinToString(", ") ?: "[]"
                    ExtractionStrategy.JOIN -> result[rule.name]?.joinToString(rule.joinSeparator ?: ", ") ?: ""
                }
                isPreviewing = false
            }
        }
    }

    fun runPreview() {
        if (rules.isEmpty() || previewHtml.isBlank()) return
        isPreviewing = true
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val engine = com.crawler.domain.engine.ExtractionEngineImpl()
            val result = engine.extract(previewHtml, rules.toList())
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                previewResult = result.map { "${it.key}: ${it.value.joinToString(", ")}" }.joinToString("\n")
                isPreviewing = false
            }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("预览结果", text)
        clipboard.primaryClip = clip
    }
}

@Composable
fun PostProcessorSection(postProcessors: MutableList<PostProcessor>) {
    var ppType by remember { mutableStateOf(PostProcessorType.TRIM) }
    var ppPattern by remember { mutableStateOf("") }
    var ppReplacement by remember { mutableStateOf("") }
    var ppTargetType by remember { mutableStateOf("STRING") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("后处理器:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            DropdownMenuButton(
                text = ppType.name,
                items = PostProcessorType.values().map { it.name },
                onSelect = { ppType = PostProcessorType.valueOf(it) },
                modifier = Modifier.weight(1f)
            )
        }

        when (ppType) {
            PostProcessorType.REGEX_REPLACE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ppPattern,
                        onValueChange = { ppPattern = it },
                        label = { Text("正则模式") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ppReplacement,
                        onValueChange = { ppReplacement = it },
                        label = { Text("替换为") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PostProcessorType.TYPE_CONVERSION -> {
                DropdownMenuButton(
                    text = ppTargetType,
                    items = listOf("STRING", "INTEGER", "LONG", "DOUBLE", "BOOLEAN"),
                    onSelect = { ppTargetType = it }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                val newPp = when (ppType) {
                    PostProcessorType.TRIM -> PostProcessor(PostProcessorType.TRIM)
                    PostProcessorType.REGEX_REPLACE -> PostProcessor(PostProcessorType.REGEX_REPLACE, ppPattern, ppReplacement)
                    PostProcessorType.TYPE_CONVERSION -> PostProcessor(PostProcessorType.TYPE_CONVERSION, targetType = ppTargetType)
                }
                postProcessors.add(newPp)
                ppPattern = ""
                ppReplacement = ""
            }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
                Text("添加处理器")
            }
        }

        if (postProcessors.isNotEmpty()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text("已添加:", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            postProcessors.forEachIndexed { idx, pp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (pp.type) {
                            PostProcessorType.TRIM -> "Trim"
                            PostProcessorType.REGEX_REPLACE -> "Regex: ${pp.pattern} → ${pp.replacement}"
                            PostProcessorType.TYPE_CONVERSION -> "To ${pp.targetType}"
                        },
                        fontSize = 12.sp
                    )
                    IconButton(onClick = { postProcessors.removeAt(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }
}

@Composable
fun RuleListItem(
    rule: ExtractionRule,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onPreview: (ExtractionRule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = CrawlerTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = rule.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Chip(
                    onClick = { /* noop */ },
                    modifier = Modifier
                ) {
                    Text(rule.selectorType.name, fontSize = 10.sp)
                }
            }
            Text(
                text = "${rule.selector} @${rule.attribute}",
                fontSize = 12.sp,
                color = CrawlerTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "策略: ${rule.strategy.name}${if (rule.strategy == ExtractionStrategy.JOIN) " (${rule.joinSeparator})" else ""}",
                fontSize = 12.sp,
                color = CrawlerTheme.colorScheme.onSurfaceVariant
            )
            if (rule.postProcessors.isNotEmpty()) {
                Text(
                    text = "后处理: ${rule.postProcessors.size} 个",
                    fontSize = 12.sp,
                    color = CrawlerTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onPreview(rule) }) {
                    Icon(Icons.Default.Preview, contentDescription = "预览")
                }
                IconButton(onClick = { onEdit(rules.indexOf(rule)) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { onDelete(rules.indexOf(rule)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
fun DropdownMenuButton(
    text: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = text

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = { /* 不直接编辑 */ },
            label = { Text("") },
            modifier = Modifier
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