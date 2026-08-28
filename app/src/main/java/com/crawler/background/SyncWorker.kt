package com.crawler.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crawler.data.entity.toDomain
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.SyncConfig
import com.crawler.domain.model.SyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val resultRepository: ResultRepository,
    private val syncService: SyncService
) : CoroutineWorker(appContext, workerParams) {

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