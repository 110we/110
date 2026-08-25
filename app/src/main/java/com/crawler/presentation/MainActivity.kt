package com.crawler.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomNavigation
import androidx.compose.material3.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.navigation.NavController
import androidx.compose.navigation.NavDestination
import androidx.compose.navigation.NavGraph
import androidx.compose.navigation.compose.NavHost
import androidx.compose.navigation.compose.composable
import androidx.compose.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crawler.presentation.ui.navigation.NavigationGraph
import com.crawler.presentation.ui.theme.CrawlerTheme
import com.crawler.presentation.viewmodel.CrawlViewModel
import com.crawler.presentation.viewmodel.PermissionViewModel
import com.crawler.presentation.viewmodel.SettingsViewModel
import com.crawler.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModel()
    private val crawlViewModel: CrawlViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val permissionViewModel: PermissionViewModel by viewModel()

    private val progressChannel = Channel<com.crawler.domain.model.CrawlProgress>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrawlerTheme {
                val navController = rememberNavController()
                val currentDestination = navController.currentBackStackEntryAsFlow()
                    .map { it?.destination?.route ?: NavigationGraph.ROOT }

                // 监听爬取进度广播
                lifecycleScope.launch {
                    for (progress in progressChannel) {
                        crawlViewModel.updateProgress(progress)
                    }
                }

                // 注册广播接收器
                val progressReceiver = android.content.BroadcastReceiver { _, intent ->
                    val progress = intent.getParcelableExtra<com.crawler.domain.model.CrawlProgress>(
                        com.crawler.background.CrawlForegroundService.EXTRA_PROGRESS
                    )
                    progress?.let { progressChannel.trySend(it) }
                }
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .registerReceiver(progressReceiver, android.content.IntentFilter(com.crawler.background.CrawlForegroundService.ACTION_PROGRESS))

                // 爬取完成广播
                val completeReceiver = android.content.BroadcastReceiver { _, intent ->
                    val taskId = intent.getStringExtra(com.crawler.background.CrawlForegroundService.EXTRA_TASK_ID) ?: ""
                    crawlViewModel.onCrawlCompleted("")
                    taskViewModel.loadTasks()
                }
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                    .registerReceiver(completeReceiver, android.content.IntentFilter("com.crawler.CRAWL_COMPLETED"))

                NavHost(navController, startDestination = NavigationGraph.ROOT) {
                    NavigationGraph.buildGraph(
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
                        }
                    )
                }

                // 底部导航栏
                val navBackStackEntry by navController.getBackStackEntry(NavigationGraph.ROOT)
                val currentRoute = navBackStackEntry.destination.route

                BottomNavigation {
                    val items = listOf(
                        NavigationGraph.ROOT to "任务" to androidx.compose.material.icons.Icons.Filled.ListAlt,
                        NavigationGraph.RESULTS to "结果" to androidx.compose.material.icons.Icons.Filled.TableChart,
                        NavigationGraph.SETTINGS to "设置" to androidx.compose.material.icons.Icons.Filled.Settings
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
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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