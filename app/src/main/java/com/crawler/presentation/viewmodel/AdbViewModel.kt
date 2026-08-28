package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.domain.repository.AdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

@HiltViewModel
class AdbViewModel @Inject constructor(
    private val adbRepository: AdbRepository
) : ViewModel() {

    data class AdbUiState(
        val shizukuAvailable: Boolean = false,
        val shizukuAuthorized: Boolean = false,
        val shizukuInstalled: Boolean = false,
        val rootAvailable: Boolean = false,
        val activeMode: String = "未就绪",
        val lastCommand: String? = null,
        val lastOutput: String? = null,
        val busy: Boolean = false
    )

    private val _uiState = MutableStateFlow(AdbUiState())
    val uiState: StateFlow<AdbUiState> = _uiState

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    init {
        permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                refreshStatus()
            }
        }
        runCatching {
            permissionListener?.let { Shizuku.addRequestPermissionResultListener(it) }
        }
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val state = _uiState.value
            val shizukuAvailable = adbRepository.isShizukuAvailable
            val shizukuAuthorized = adbRepository.isShizukuAuthorized
            val shizukuInstalled = adbRepository.isShizukuInstalled
            val rootAvailable = adbRepository.isRootAvailable
            val activeMode = when {
                shizukuAuthorized -> "Shizuku"
                rootAvailable -> "Root"
                else -> "本地"
            }
            _uiState.value = state.copy(
                shizukuAvailable = shizukuAvailable,
                shizukuAuthorized = shizukuAuthorized,
                shizukuInstalled = shizukuInstalled,
                rootAvailable = rootAvailable,
                activeMode = activeMode
            )
        }
    }

    fun openShizukuApp() {
        adbRepository.openShizukuApp()
    }

    fun requestShizukuPermission() {
        if (!adbRepository.isShizukuAvailable) return
        if (adbRepository.isShizukuAuthorized) {
            refreshStatus()
            return
        }
        runCatching {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun runTestCommand(command: String = "id") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true)
            val result = runCatching { adbRepository.execute(command) }
                .getOrElse { e ->
                    com.crawler.data.adb.AdbCommandResult(-1, "", e.message ?: "error")
                }
            _uiState.value = _uiState.value.copy(
                busy = false,
                lastCommand = command,
                lastOutput = "exit=${result.exitCode}\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}"
            )
        }
    }

    override fun onCleared() {
        permissionListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        permissionListener = null
        super.onCleared()
    }

    companion object {
        private const val REQUEST_CODE = 10001
    }
}
