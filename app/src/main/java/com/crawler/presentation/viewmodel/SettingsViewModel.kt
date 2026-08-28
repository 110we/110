package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.data.entity.AppSettingsEntity
import com.crawler.data.repository.SettingsRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.service.TaskBackupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val taskBackupService: TaskBackupService
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettingsEntity>(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        loadSettings()
    }

    fun saveSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            _isSaving.value = true
            settingsRepository.updateSetting("default_user_agent", newSettings.defaultUserAgent)
            settingsRepository.updateSetting("default_timeout", newSettings.defaultTimeoutSeconds)
            settingsRepository.updateSetting("default_max_redirects", newSettings.defaultMaxRedirects)
            settingsRepository.updateSetting("default_concurrency", newSettings.defaultConcurrency)
            settingsRepository.updateSetting("global_rate_limit", newSettings.globalRateLimitPerSecond)
            settingsRepository.updateSetting("robots_txt_compliance", newSettings.robotsTxtCompliance)
            settingsRepository.updateSetting("js_rendering_default_enabled", newSettings.jsRenderingDefaultEnabled)
            settingsRepository.updateSetting("js_rendering_default_timeout", newSettings.jsRenderingDefaultTimeout)
            _isSaving.value = false
            _message.value = "设置已保存"
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            settingsRepository.getSettings()
                .subscribe(
                    { settings ->
                        _settings.value = settings
                        _isLoading.value = false
                    },
                    { error ->
                        _isLoading.value = false
                    }
                )
        }
    }

    suspend fun updateSetting(key: String, value: Any): Boolean {
        return settingsRepository.updateSetting(key, value)
    }

    suspend fun resetDefaults(): Boolean {
        val success = settingsRepository.resetDefaults()
        if (success) loadSettings()
        return success
    }

    fun clearCache() {
        _message.value = "缓存已清理"
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val success = settingsRepository.resetDefaults()
            _message.value = if (success) "设置已重置" else "重置失败"
            loadSettings()
        }
    }

    fun exportAllData() {
        viewModelScope.launch {
            val result = taskBackupService.exportTasks()
            _message.value = result.fold(
                onSuccess = { "已导出 ${result.getOrNull()?.length ?: 0} 字节任务备份" },
                onFailure = { "导出失败: ${it.message}" }
            )
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            taskRepository.exportTasks().forEach { task ->
                taskRepository.delete(task.id)
            }
            _message.value = "所有数据已删除"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
