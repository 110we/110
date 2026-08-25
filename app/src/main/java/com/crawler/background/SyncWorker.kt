package com.crawler.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.SyncConfig
import com.crawler.domain.service.SyncService
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class SyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val resultRepository: ResultRepository,
    private val syncService: SyncService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()

        val task = taskRepository.getById(taskId)?.toDomain()
            ?: return Result.failure()

        val syncConfig = task.syncConfig ?: return Result.failure()
        if (!syncConfig.enabled) return Result.success()

        val results = resultRepository.getByTaskId(taskId, limit = 1000)
        val domainResults = results.map { it.toDomain() }

        val syncResult = syncService.sync(domainResults, syncConfig)

        // 更新同步状态
        for (result in results) {
            val updated = result.copy(syncedAt = if (syncResult.success) System.currentTimeMillis() else null)
            resultRepository.update(updated)
        }

        return if (syncResult.success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

// 扩展函数
private fun com.crawler.data.entity.CrawlResultEntity.toDomain(): com.crawler.domain.model.CrawlResult {
    return com.crawler.domain.model.CrawlResult(
        id = id,
        taskId = taskId,
        url = url,
        data = com.google.gson.Gson().fromJson(extractedData, Map::class.java),
        status = com.crawler.domain.model.ResultStatus.valueOf(status.name),
        errorMessage = errorMessage,
        crawledAt = kotlinx.datetime.Instant.ofEpochMillisecond(crawledAt),
        syncedAt = syncedAt?.let { kotlinx.datetime.Instant.ofEpochMillisecond(it) }
    )
}