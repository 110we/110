package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.background.CrawlForegroundService
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.CrawlProgress
import com.crawler.domain.model.CrawlResult
import com.crawler.domain.model.CrawlStatus
import com.crawler.domain.model.CrawlTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class CrawlViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _crawlState = MutableStateFlow<CrawlStatus>(CrawlStatus.IDLE)
    val crawlState: StateFlow<CrawlStatus> = _crawlState

    private val _progress = MutableStateFlow<CrawlProgress?>(null)
    val progress: StateFlow<CrawlProgress?> = _progress

    private val _currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskId: StateFlow<String?> = _currentTaskId

    private val progressChannel = Channel<CrawlProgress>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    fun startCrawl(taskId: String) {
        if (_crawlState.value == CrawlStatus.RUNNING) return

        _currentTaskId.value = taskId
        _crawlState.value = CrawlStatus.RUNNING
        _progress.value = CrawlProgress(
            taskId = taskId,
            currentUrl = "正在启动...",
            pagesCrawled = 0,
            itemsExtracted = 0,
            errors = 0,
            elapsedMs = 0,
            status = CrawlStatus.RUNNING
        )

        // 启动前台服务
        val context = com.crawler.CrawlerApplication.instance!!
        val intent = android.content.Intent(context, CrawlForegroundService::class.java).apply {
            putExtra(CrawlForegroundService.EXTRA_TASK_ID, taskId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // 监听进度通道
        viewModelScope.launch {
            for (p in progressChannel) {
                _progress.value = p
            }
        }
    }

    fun stopCrawl() {
        _crawlState.value = CrawlStatus.STOPPED
        val context = com.crawler.CrawlerApplication.instance!!
        val intent = android.content.Intent(context, CrawlForegroundService::class.java).apply {
            action = CrawlForegroundService.ACTION_STOP
        }
        context.startService(intent)
        _currentTaskId.value = null
        progressChannel.close()
    }

    fun updateProgress(progress: CrawlProgress) {
        _progress.value = progress
        progressChannel.trySend(progress)
    }

    fun onCrawlCompleted(summary: String) {
        _crawlState.value = CrawlStatus.COMPLETED
        _progress.value = _progress.value?.copy(status = CrawlStatus.COMPLETED)
        progressChannel.close()
    }

    fun onCrawlError(error: String) {
        _crawlState.value = CrawlStatus.FAILED
        _progress.value = _progress.value?.copy(status = CrawlStatus.FAILED)
        progressChannel.close()
    }

    fun clearCurrentTask() {
        _currentTaskId.value = null
        _progress.value = null
        _crawlState.value = CrawlStatus.IDLE
    }
}

// 需要在 CrawlerApplication 中添加 instance 字段