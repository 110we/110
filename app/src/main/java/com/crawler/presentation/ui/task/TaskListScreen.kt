package com.crawler.presentation.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Chip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.presentation.viewmodel.TaskViewModel
import com.crawler.R
import com.crawler.domain.model.CrawlStatus
import com.crawler.presentation.ui.theme.CrawlerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.spring
import androidx.compose.animation.tween

@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onNewTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onRuleBuilder: (String) -> Unit,
    onResults: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskViewModel: TaskViewModel = viewModel()
    val tasks by taskViewModel.tasks.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val error by taskViewModel.error.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )
        } else if (tasks.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "暂无任务，点击右下角创建",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    TaskItemCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onEdit = { onEditTask(task.id) },
                        onRules = { onRuleBuilder(task.id) },
                        onResults = { onResults(task.id) },
                        onDelete = {
                            taskViewModel.deleteTask(task.id).onSuccess {
                                // 删除成功，列表会自动刷新
                            }
                        }
                    )
                }
            }
        }

        // 悬浮按钮
        FloatingActionButton(
            onClick = onNewTask,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新建任务")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemCard(
    task: com.crawler.domain.model.CrawlTask,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRules: () -> Unit,
    onResults: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.scheduleConfig?.enabled == true) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        onClick = onClick,
        elevation = androidx.compose.foundation.layout.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "URLs: ${task.baseUrls.size} | 规则: ${task.extractionRules.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Chip(
                    onClick = { /* 状态芯片不可点击 */ },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = androidx.compose.material3.ChipDefaults.chipColors(
                        containerColor = statusColor.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = if (task.scheduleConfig?.enabled == true) "定时" else "手动",
                        fontSize = 12.sp,
                        color = statusColor
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑", fontSize = 14.sp)
                }

                Button(
                    onClick = onRules,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "规则", modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                    Text("规则", fontSize = 14.sp)
                }

                Button(
                    onClick = onResults,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "结果", modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                    Text("结果", fontSize = 14.sp)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}