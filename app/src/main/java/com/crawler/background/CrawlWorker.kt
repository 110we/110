package com.crawler.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.crawler.data.history.CrawlHistoryEntity
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.CrawlEngine
import com.crawler.domain.model.CrawlProgress
import com.crawler.domain.model.CrawlStatus
import com.crawler.domain.repository.AdbRepository
import com.crawler.domain.repository.HistoryRepository
import javax.inject.Inject

class CrawlWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val resultRepository: ResultRepository,
    private val crawlEngine: CrawlEngine,
    private val adbRepository: AdbRepository,
    private val historyRepository: HistoryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val cronExpression = inputData.getString("cron") ?: ""

        val task = taskRepository.getById(taskId)?.toDomain()
            ?: return Result.failure()

        // 若任务需要读取受保护数据目录，则通过 ADB 前置检查授权状态
        val adbReady = adbRepository.isShizukuAuthorized || adbRepository.isRootAvailable

        // 记录执行历史（开始）
        val historyId = historyRepository.recordStart(
            taskId = task.id,
            taskName = task.name,
            triggerType = if (cronExpression.isNotBlank()) "SCHEDULED" else "MANUAL"
        )

        // 静默执行，无通知
        val summary = crawlEngine.execute(task) { progress ->
            // 可选：上报进度到 WorkManager
            setProgressAsync(workDataOf(
                "pages" to progress.pagesCrawled,
                "items" to progress.itemsExtracted,
                "errors" to progress.errors,
                "adb_ready" to adbReady
            ))
        }

        // 记录执行历史（完成）
        val finalStatus = when {
            summary.status == CrawlStatus.COMPLETED -> CrawlHistoryEntity.STATUS_COMPLETED
            summary.status == CrawlStatus.FAILED -> CrawlHistoryEntity.STATUS_FAILED
            else -> CrawlHistoryEntity.STATUS_STOPPED
        }
        historyRepository.recordCompletion(
            historyId = historyId,
            status = finalStatus,
            pagesCrawled = summary.totalPages,
            itemsExtracted = summary.totalItems,
            errors = summary.totalErrors
        )

        // 保存结果摘要
        // 实际项目中会在 CrawlEngine 内部逐条保存

        // 如果是 cron 任务，重新调度
        if (cronExpression.isNotBlank()) {
            rescheduleCron(task, cronExpression)
        }

        return if (summary.status == CrawlStatus.COMPLETED) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun rescheduleCron(task: com.crawler.domain.model.CrawlTask, cron: String) {
        // 这里简化处理，实际需要用 WorkManager 的周期性工作或手动重新计算下次执行时间
        // 暂不实现完整的 cron 重调度
    }
}

// TaskEntity 扩展函数
internal fun com.crawler.data.entity.CrawlTaskEntity.toDomain(): com.crawler.domain.model.CrawlTask {
    return com.crawler.domain.model.CrawlTask(
        id = id,
        name = name,
        baseUrls = baseUrls,
        urlPatterns = com.crawler.domain.model.UrlPatterns(
            includePatterns = urlPatterns.includePatterns,
            excludePatterns = urlPatterns.excludePatterns,
            maxDepth = urlPatterns.maxDepth,
            maxPages = urlPatterns.maxPages
        ),
        extractionRules = extractionRules.map { it.toDomain() },
        requestConfig = requestConfig.toDomain(),
        scheduleConfig = scheduleConfig?.toDomain(),
        jsRenderingConfig = jsRenderingConfig?.toDomain(),
        syncConfig = syncConfig?.toDomain(),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt)
    )
}

internal fun com.crawler.data.entity.ExtractionRuleEntity.toDomain(): com.crawler.domain.model.ExtractionRule {
    return com.crawler.domain.model.ExtractionRule(
        fieldName = fieldName,
        selectorType = com.crawler.domain.model.SelectorType.valueOf(selectorType.name),
        expression = expression,
        attribute = attribute,
        multiple = com.crawler.domain.model.MultipleStrategy.valueOf(multiple.name),
        joinDelimiter = joinDelimiter,
        postProcessors = postProcessors.map { it.toDomain() }
    )
}

internal fun com.crawler.data.entity.PostProcessorEntity.toDomain(): com.crawler.domain.model.PostProcessor {
    return when (this) {
        is com.crawler.data.entity.PostProcessorEntity.Trim -> com.crawler.domain.model.PostProcessor.Trim(enabled)
        is com.crawler.data.entity.PostProcessorEntity.RegexReplace -> com.crawler.domain.model.PostProcessor.RegexReplace(pattern, replacement)
        is com.crawler.data.entity.PostProcessorEntity.TypeConversion -> com.crawler.domain.model.PostProcessor.TypeConversion(
            com.crawler.domain.model.PostProcessor.DataType.valueOf(targetType.name)
        )
    }
}

internal fun com.crawler.data.entity.RequestConfigEntity.toDomain(): com.crawler.domain.model.RequestConfig {
    return com.crawler.domain.model.RequestConfig(
        method = com.crawler.domain.model.HttpMethod.valueOf(method.name),
        headers = headers,
        cookies = cookies,
        body = body,
        bodyType = com.crawler.domain.model.BodyType.valueOf(bodyType.name),
        timeoutSeconds = timeoutSeconds,
        followRedirects = followRedirects,
        maxRedirects = maxRedirects,
        userAgent = userAgent
    )
}

internal fun com.crawler.data.entity.ScheduleConfigEntity.toDomain(): com.crawler.domain.model.ScheduleConfig {
    return com.crawler.domain.model.ScheduleConfig(
        type = com.crawler.domain.model.ScheduleType.valueOf(type.name),
        cronExpression = cronExpression,
        timeOfDay = timeOfDay,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        enabled = enabled
    )
}

internal fun com.crawler.data.entity.JsRenderingConfigEntity.toDomain(): com.crawler.domain.model.JsRenderingConfig {
    return com.crawler.domain.model.JsRenderingConfig(
        enabled = enabled,
        waitCondition = com.crawler.domain.model.WaitCondition.valueOf(waitCondition.name),
        waitSelector = waitSelector,
        waitScript = waitScript,
        timeoutSeconds = timeoutSeconds,
        blockResources = blockResources
    )
}

internal fun com.crawler.data.entity.SyncConfigEntity.toDomain(): com.crawler.domain.model.SyncConfig {
    return com.crawler.domain.model.SyncConfig(
        enabled = enabled,
        endpoint = endpoint,
        authType = com.crawler.domain.model.AuthType.valueOf(authType.name),
        credentials = com.crawler.domain.model.EncryptedCredentials(credentials.username, credentials.password),
        payloadFormat = com.crawler.domain.model.PayloadFormat.valueOf(payloadFormat.name),
        syncOnComplete = syncOnComplete
    )
}