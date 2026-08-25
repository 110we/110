package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionHelper: PermissionHelper
) : ViewModel() {

    private val _permissionStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionStatus: StateFlow<Map<String, Boolean>> = _permissionStatus

    private val _clipboardMessage = MutableStateFlow<String?>(null)
    val clipboardMessage: StateFlow<String?> = _clipboardMessage

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _permissionStatus.value = permissionHelper.getPermissionStatus()
        }
    }

    fun openManageStorageSettings() {
        permissionHelper.openManageStorageSettings()
    }

    fun openUsageStatsSettings() {
        permissionHelper.openUsageStatsSettings()
    }

    fun openInstallPackagesSettings() {
        permissionHelper.openInstallPackagesSettings()
    }

    fun copyAdbCommands() {
        val success = permissionHelper.copyAdbCommandsToClipboard(
            com.crawler.CrawlerApplication.instance.packageName
        )
        _clipboardMessage.value = if (success) "ADB 命令已复制到剪贴板" else "复制失败"
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _clipboardMessage.value = null
        }
    }

    fun isPermissionGranted(permission: String): Boolean {
        return _permissionStatus.value[permission] ?: false
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            "MANAGE_EXTERNAL_STORAGE" -> "允许导出文件到任意目录，无需系统选择器"
            "QUERY_ALL_PACKAGES" -> "检测已安装的浏览器/代理工具"
            "PACKAGE_USAGE_STATS" -> "智能调度优化（可选）"
            "REQUEST_INSTALL_PACKAGES" -> "安装插件 APK / 自我更新"
            else -> ""
        }
    }
}