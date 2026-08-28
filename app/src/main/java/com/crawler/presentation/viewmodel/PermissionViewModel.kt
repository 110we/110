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

    private val _permissionsState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionsState: StateFlow<Map<String, Boolean>> = _permissionsState

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    private val _clipboardMessage = MutableStateFlow<String?>(null)
    val clipboardMessage: StateFlow<String?> = _clipboardMessage

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isChecking.value = true
            _permissionsState.value = toFullPermissionNames(permissionHelper.getPermissionStatus())
            _isChecking.value = false
        }
    }

    fun checkAllPermissions() {
        refreshStatus()
    }

    fun requestPermission(permission: String) {
        refreshStatus()
    }

    fun openAppSettings() {
        permissionHelper.openManageStorageSettings()
    }

    fun copyAdbCommand(permission: String) {
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
        return _permissionsState.value[permission] ?: false
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "允许导出文件到任意目录，无需系统选择器"
            android.Manifest.permission.QUERY_ALL_PACKAGES -> "检测已安装的浏览器/代理工具"
            android.Manifest.permission.PACKAGE_USAGE_STATS -> "智能调度优化（可选）"
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> "安装插件 APK / 自我更新"
            else -> ""
        }
    }

    private fun toFullPermissionNames(short: Map<String, Boolean>): Map<String, Boolean> {
        return mapOf(
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE to (short["MANAGE_EXTERNAL_STORAGE"] ?: false),
            android.Manifest.permission.QUERY_ALL_PACKAGES to (short["QUERY_ALL_PACKAGES"] ?: false),
            android.Manifest.permission.PACKAGE_USAGE_STATS to (short["PACKAGE_USAGE_STATS"] ?: false),
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES to (short["REQUEST_INSTALL_PACKAGES"] ?: false)
        )
    }
}
