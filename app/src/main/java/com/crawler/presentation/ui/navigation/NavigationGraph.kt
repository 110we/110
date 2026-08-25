package com.crawler.presentation.ui.navigation

import androidx.navigation.NavHost
import androidx.navigation.NavType
import androidx.navigation.composable.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crawler.presentation.ui.task.TaskListScreen
import com.crawler.presentation.ui.task.TaskEditorScreen
import com.crawler.presentation.ui.task.RuleBuilderScreen
import com.crawler.presentation.ui.task.ResultsScreen
import com.crawler.presentation.ui.settings.SettingsScreen
import com.crawler.presentation.ui.settings.PermissionStatusScreen

object NavigationGraph {
    const val ROOT = "tasks"
    const val TASK_EDITOR = "task_editor"
    const val RULE_BUILDER = "rule_builder"
    const val RESULTS = "results"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"

    fun NavGraphBuilder.buildGraph(
        navController: androidx.navigation.NavController,
        onTaskClick: (String) -> Unit,
        onNewTask: () -> Unit,
        onEditTask: (String) -> Unit,
        onRuleBuilder: (String) -> Unit,
        onResults: (String) -> Unit,
        onSettings: () -> Unit,
        onPermissions: () -> Unit
    ) {
        composable(ROOT) {
            TaskListScreen(
                onTaskClick = onTaskClick,
                onNewTask = onNewTask,
                onEditTask = onEditTask,
                onRuleBuilder = onRuleBuilder,
                onResults = onResults
            )
        }
        composable(
            route = "$TASK_EDITOR/{taskId}",
            arguments = listOf(androidx.navigation.navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.getString() ?: ""
            TaskEditorScreen(
                taskId = if (taskId.isBlank()) null else taskId,
                onSave = { _ ->
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "$RULE_BUILDER/{taskId}",
            arguments = listOf(androidx.navigation.navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.getString() ?: ""
            RuleBuilderScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "$RESULTS/{taskId}",
            arguments = listOf(androidx.navigation.navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.getString() ?: ""
            ResultsScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPermissionsClick = onPermissions
            )
        }
        composable(PERMISSIONS) {
            PermissionStatusScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}