package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.data.repository.SettingsRepository
import com.crawler.domain.model.AppSettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettingsEntity>(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSettings()
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
}