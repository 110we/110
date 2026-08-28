package com.crawler.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.crawler.R
import com.crawler.data.entity.toDomain
import com.crawler.data.history.CrawlHistoryEntity
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.CrawlEngine
import com.crawler.domain.model.CrawlProgress
import com.crawler.domain.model.CrawlStatus
import com.crawler.domain.repository.HistoryRepository
import com.crawler.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class CrawlForegroundService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "crawl_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.crawler.ACTION_STOP_CRAWL"
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_PROGRESS = "com.crawler.ACTION_CRAWL_PROGRESS"
        const val EXTRA_PROGRESS = "progress"
    }

    @Inject
    lateinit var taskRepository: TaskRepository
    @Inject
    lateinit var resultRepository: ResultRepository
    @Inject
    lateinit var crawlEngine: CrawlEngine
    @Inject
    lateinit var historyRepository: HistoryRepository

    private var currentTaskId: String? = null
    private var currentTaskName: String? = null
    private var currentHistoryId: String? = null
    private var crawlJob: Job? = null
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCrawl()
            return START_NOT_STICKY
        }

        currentTaskId = intent?.getStringExtra(EXTRA_TASK_ID)
        currentTaskId?.let { startCrawl(it) }

        // Android 14+ 需显式调用 startForeground 并指定类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notification = buildInitialNotification()
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }

        return START_STICKY
    }

    private fun buildInitialNotification(): Notification {
        createNotificationChannel()
        val stopIntent = Intent(this, CrawlForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("爬虫任务")
            .setContentText("正在启动...")
            .setSmallIcon(R.drawable.ic_crawl_notification)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun startCrawl(taskId: String) {
        showProgressNotification("正在启动...", 0, 0, 0, 0, CrawlStatus.RUNNING)

        crawlJob = CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getById(taskId)?.toDomain()
            if (task == null) {
                updateNotificationError("任务不存在")
                stopSelf()
                return@launch
            }
            currentTaskName = task.name

            // 记录执行历史（开始）
            currentHistoryId = historyRepository.recordStart(
                taskId = task.id,
                taskName = task.name,
                triggerType = "MANUAL"
            )

            val summary = crawlEngine.execute(task) { progress ->
                updateProgressNotification(progress)
                // 广播进度给 UI
                val intent = Intent(ACTION_PROGRESS).apply {
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_PROGRESS, progress)
                }
                sendBroadcast(intent)
            }

            // 记录执行历史（完成）
            val finalStatus = when {
                summary.status == CrawlStatus.COMPLETED -> CrawlHistoryEntity.STATUS_COMPLETED
                summary.status == CrawlStatus.FAILED -> CrawlHistoryEntity.STATUS_FAILED
                else -> CrawlHistoryEntity.STATUS_STOPPED
            }
            currentHistoryId?.let { historyId ->
                historyRepository.recordCompletion(
                    historyId = historyId,
                    status = finalStatus,
                    pagesCrawled = summary.totalPages,
                    itemsExtracted = summary.totalItems,
                    errors = summary.totalErrors
                )
            }

            // 爬取完成
            val finalMessage = "爬取完成: ${summary.totalPages} 页面, ${summary.totalItems} 条目, ${summary.totalErrors} 错误"
            showProgressNotification(finalMessage, summary.totalPages, summary.totalItems, summary.totalErrors, 0, summary.status)

            // 发送完成广播
            sendBroadcast(Intent("com.crawler.CRAWL_COMPLETED").putExtra(EXTRA_TASK_ID, taskId))

            stopSelf()
        }
    }

    private fun stopCrawl() {
        crawlJob?.cancel()
        crawlJob = null
        // 记录停止状态
        currentHistoryId?.let { historyId ->
            CoroutineScope(Dispatchers.IO).launch {
                historyRepository.recordCompletion(
                    historyId = historyId,
                    status = CrawlHistoryEntity.STATUS_STOPPED,
                    pagesCrawled = 0,
                    itemsExtracted = 0,
                    errors = 0
                )
            }
        }
        showProgressNotification("已停止", 0, 0, 0, 0, CrawlStatus.STOPPED)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "爬虫进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示爬虫任务的实时进度"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(
        text: String,
        pages: Int,
        items: Long,
        errors: Int,
        elapsedMs: Long,
        status: CrawlStatus
    ) {
        val stopIntent = Intent(this, CrawlForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val progress = if (pages > 0) (pages * 100 / (pages + errors + 1)).coerceIn(0, 100) else 0

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("爬虫任务: ${getTaskName(currentTaskId)}")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_crawl_notification)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setOngoing(status == CrawlStatus.RUNNING)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        notificationManager?.notify(NOTIFICATION_ID, builder.build())

        if (status != CrawlStatus.RUNNING) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(5000)
                notificationManager?.cancel(NOTIFICATION_ID)
            }
        }
    }

    private fun updateProgressNotification(progress: CrawlProgress) {
        val elapsed = formatElapsed(progress.elapsedMs)
        val text = "当前: ${progress.currentUrl ?: "等待中..."}\n" +
                "页面: ${progress.pagesCrawled} | 条目: ${progress.itemsExtracted} | 错误: ${progress.errors} | 耗时: $elapsed"
        showProgressNotification(
            text,
            progress.pagesCrawled,
            progress.itemsExtracted,
            progress.errors,
            progress.elapsedMs,
            progress.status
        )
    }

    private fun updateNotificationError(error: String) {
        showProgressNotification("错误: $error", 0, 0, 0, 0, CrawlStatus.FAILED)
    }

    private fun getTaskName(taskId: String?): String {
        return currentTaskName ?: taskId ?: "未知"
    }

    private fun formatElapsed(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else String.format("%02d:%02d", minutes % 60, seconds % 60)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        crawlJob?.cancel()
        notificationManager?.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }
}