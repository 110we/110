package com.crawler.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.crawler.presentation.ui.navigation.NavigationGraph
import com.crawler.presentation.ui.navigation.buildGraph
import com.crawler.presentation.ui.theme.CrawlerTheme
import com.crawler.presentation.viewmodel.CrawlViewModel
import com.crawler.presentation.viewmodel.PermissionViewModel
import com.crawler.presentation.viewmodel.SettingsViewModel
import com.crawler.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val crawlViewModel: CrawlViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private val progressChannel = Channel<com.crawler.domain.model.CrawlProgress>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrawlerTheme {
                val navController = rememberNavController()

                // 监听爬取进度广播
                lifecycleScope.launch {
                    for (progress in progressChannel) {
                        crawlViewModel.updateProgress(progress)
                    }
                }

                // 注册广播接收器
                val progressReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                        val progress = intent?.getParcelableExtra<com.crawler.domain.model.CrawlProgress>(
                            com.crawler.background.CrawlForegroundService.EXTRA_PROGRESS
                        )
                        progress?.let { progressChannel.trySend(it) }
                    }
                }
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .registerReceiver(progressReceiver, android.content.IntentFilter(com.crawler.background.CrawlForegroundService.ACTION_PROGRESS))

                // 爬取完成广播
                val completeReceiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                        val taskId = intent?.getStringExtra(com.crawler.background.CrawlForegroundService.EXTRA_TASK_ID) ?: ""
                        crawlViewModel.onCrawlCompleted("")
                        taskViewModel.loadTasks()
                    }
                }
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .registerReceiver(completeReceiver, android.content.IntentFilter("com.crawler.CRAWL_COMPLETED"))

                NavHost(navController, startDestination = NavigationGraph.ROOT) {
                    buildGraph(
                        navController = navController,
                        onTaskClick = { taskId ->
                            navController.navigate(NavigationGraph.RESULTS + "/$taskId")
                        },
                        onNewTask = {
                            navController.navigate(NavigationGraph.TASK_EDITOR + "/new")
                        },
                        onEditTask = { taskId ->
                            navController.navigate(NavigationGraph.TASK_EDITOR + "/$taskId")
                        },
                        onRuleBuilder = { taskId ->
                            navController.navigate(NavigationGraph.RULE_BUILDER + "/$taskId")
                        },
                        onResults = { taskId ->
                            navController.navigate(NavigationGraph.RESULTS + "/$taskId")
                        },
                        onSettings = {
                            navController.navigate(NavigationGraph.SETTINGS)
                        },
                        onPermissions = {
                            navController.navigate(NavigationGraph.PERMISSIONS)
                        },
                        onAdbStatus = {
                            navController.navigate(NavigationGraph.ADB_STATUS)
                        },
                        onHistory = {
                            navController.navigate(NavigationGraph.HISTORY)
                        }
                    )
                }

                // 底部导航栏
                val navBackStackEntry = navController.getBackStackEntry(NavigationGraph.ROOT)
                val currentRoute = navBackStackEntry.destination.route

                BottomNavigation {
                    val items: List<Triple<String, String, ImageVector>> = listOf(
                        Triple(NavigationGraph.ROOT, "任务", Icons.Filled.Home),
                        Triple(NavigationGraph.RESULTS, "结果", Icons.Filled.DateRange),
                        Triple(NavigationGraph.SETTINGS, "设置", Icons.Filled.Settings)
                    )
                    items.forEach { (route, label, icon) ->
                        val selected = currentRoute?.startsWith(route) == true
                        BottomNavigationItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun enableEdgeToEdge() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}