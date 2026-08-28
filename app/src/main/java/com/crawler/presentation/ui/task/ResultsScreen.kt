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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material.Chip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.domain.model.CrawlResult
import com.crawler.domain.model.ExportFormat
import com.crawler.presentation.viewmodel.ResultsViewModel
import com.crawler.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    taskId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resultsViewModel: ResultsViewModel = viewModel()
    val taskViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.crawler.presentation.viewmodel.TaskViewModel>()

    var searchQuery by remember { mutableStateOf("") }
    var selectedColumns by remember { mutableStateOf<Set<String>>(setOf()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(ExportFormat.JSON) }
    var showColumnSelector by remember { mutableStateOf(false) }

    val task = taskViewModel.tasks.value.firstOrNull { it.id == taskId }
    val results = resultsViewModel.results.collectAsState().value
    val isLoading = resultsViewModel.isLoading.collectAsState().value
    val error = resultsViewModel.error.collectAsState().value

    // 初始化列选择
    androidx.compose.runtime.LaunchedEffect(results) {
        if (results.isNotEmpty() && selectedColumns.isEmpty()) {
            val firstResult = results.first()
            val columns = firstResult.extractedData.keys.toSet()
            selectedColumns = columns
        }
    }

    // 可用的列（从所有结果收集）
    val allColumns = remember(results) {
        results.flatMap { it.extractedData.keys }.toSet().toList().sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("爬取结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showColumnSelector = true }) {
                        Icon(Icons.Default.ViewColumn, contentDescription = "列选择")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "导出")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 搜索栏
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("搜索结果...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                        Chip(
                            onClick = { /* 高级筛选 */ },
                            modifier = Modifier
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "筛选")
                            Text("筛选")
                        }
                    }
                }

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

                // 结果表格
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        Text("加载中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text(
                            text = "暂无结果",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // 过滤结果
                    val filteredResults = remember(searchQuery, results) {
                        if (searchQuery.isBlank()) results
                        else results.filter { result ->
                            result.extractedData.values.any { it.toString().contains(searchQuery, ignoreCase = true) }
                                || result.url.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    val displayColumns = remember(selectedColumns, allColumns) {
                        if (selectedColumns.isEmpty()) allColumns else allColumns.filter { it in selectedColumns }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f, true),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // 表头
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayColumns.forEach { col ->
                                    Text(
                                        text = col,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                }
                                Text(
                                    text = "操作",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }

                        // 数据行
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, true),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(filteredResults) { result ->
                                ResultRow(
                                    result = result,
                                    columns = displayColumns,
                                    onDetailClick = { showDetailDialog(result) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 列选择弹窗
    if (showColumnSelector) {
        ColumnSelectorDialog(
            allColumns = allColumns,
            selectedColumns = selectedColumns,
            onDismiss = { showColumnSelector = false }
        )
    }

    // 导出弹窗
    if (showExportDialog) {
        ExportDialog(
            taskName = task?.name ?: "results",
            onExport = { format ->
                exportFormat = format
                resultsViewModel.exportResults(taskId!!, format)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // 详情弹窗
    var detailResult by remember { mutableStateOf<CrawlResult?>(null) }
    if (detailResult != null) {
        DetailDialog(
            result = detailResult!!,
            onDismiss = { detailResult = null }
        )
    }

    fun showDetailDialog(result: CrawlResult) {
        detailResult = result
    }
}

@Composable
fun ResultRow(
    result: CrawlResult,
    columns: List<String>,
    onDetailClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { col ->
                val value = result.extractedData[col]?.toString() ?: ""
                Text(
                    text = if (value.length > 50) "${value.substring(0, 50)}..." else value,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            IconButton(onClick = onDetailClick) {
                Icon(Icons.Default.Visibility, contentDescription = "详情")
            }
        }
    }
}

@Composable
fun ColumnSelectorDialog(
    allColumns: List<String>,
    selectedColumns: MutableState<Set<String>>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
        Card(
            modifier = Modifier
                .width(300.dp)
                .height(400.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("选择显示列", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f, true),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allColumns) { col ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { selectedColumns.value = selectedColumns.value.toggle(col) }
                        ) {
                            Checkbox(
                                checked = col in selectedColumns.value,
                                onCheckedChange = { selectedColumns.value = selectedColumns.value.toggle(col) }
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                            Text(col, fontSize = 14.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(
    taskName: String,
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
        Card(
            modifier = Modifier
                .width(350.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("导出结果", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("任务: $taskName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportFormat.values().forEach { format ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(format.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(format.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { onExport(format) }) {
                                Text("导出")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailDialog(
    result: CrawlResult,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
        Card(
            modifier = Modifier
                .width(500.dp)
                .height(600.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Text("URL: ${result.url}", fontSize = 14.sp)
                Text("状态: ${result.status.name}", fontSize = 14.sp)
                Text("时间: ${result.timestamp}", fontSize = 14.sp)
                if (result.errorMessage != null) {
                    Text("错误: ${result.errorMessage}", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text("提取数据:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f, true),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(result.extractedData.entries.sortedBy { it.key }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(entry.key, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.value.toString(),
                                    fontSize = 13.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

fun <T> Set<T>.toggle(element: T): Set<T> {
    return if (contains(element)) this - element else this + element
}