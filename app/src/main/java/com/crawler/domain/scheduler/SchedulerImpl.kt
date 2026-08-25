package com.crawler.domain.scheduler

import android.content.Context
import androidx.work.*
import com.crawler.background.CrawlWorker
import com.crawler.domain.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import it.sauronsoftware.cron4j.SchedulingPattern
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerImpl @Inject constructor(
    private val context: Context
) : Scheduler {

    private val workManager = WorkManager.getInstance(context)

    override suspend fun schedule(task: CrawlTask): Result<ScheduleInfo> {
        val scheduleConfig = task.scheduleConfig ?: return Result.failure(Exception("No schedule config"))

        val workRequest = when (scheduleConfig.type) {
            ScheduleType.ONCE -> buildOneTimeWork(task, scheduleConfig)
            ScheduleType.DAILY -> buildPeriodicWork(task, scheduleConfig, 24, TimeUnit.HOURS)
            ScheduleType.WEEKLY -> buildPeriodicWork(task, scheduleConfig, 7, TimeUnit.DAYS)
            ScheduleType.MONTHLY -> buildPeriodicWork(task, scheduleConfig, 30, TimeUnit.DAYS)
            ScheduleType.CUSTOM -> buildCronWork(task, scheduleConfig)
        }

        val workId = workRequest.id.toString()
        try {
            workManager.enqueueUniqueWork(workId, ExistingWorkPolicy.REPLACE, workRequest).await()
            val nextRun = calculateNextRun(scheduleConfig)
            return Result.success(ScheduleInfo(nextRun, workId))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun cancel(taskId: String) {
        workManager.cancelUniqueWork(taskId)
    }

    override suspend fun reschedule(task: CrawlTask) {
        cancel(task.id)
        schedule(task)
    }

    override suspend fun getNextRun(taskId: String): Instant? {
        val info = workManager.getWorkInfoByIdLiveData(taskId).await()
        return info?.runAttempt?.let { _ ->
            // Simplified - would need to track actual next run time
            Instant.now().plusSeconds(3600)
        }
    }

    private fun buildOneTimeWork(task: CrawlTask, config: ScheduleConfig): OneTimeWorkRequest {
        val delay = calculateInitialDelay(config)
        return OneTimeWorkRequestBuilder<CrawlWorker>()
            .setInputData(workDataOf("task_id" to task.id))
            .setInitialDelay(delay.toLong(), TimeUnit.MILLISECONDS)
            .setConstraints(buildConstraints())
            .build()
    }

    private fun buildPeriodicWork(
        task: CrawlTask,
        config: ScheduleConfig,
        interval: Long,
        unit: TimeUnit
    ): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<CrawlWorker>(interval, unit)
            .setInputData(workDataOf("task_id" to task.id))
            .setConstraints(buildConstraints())
            .build()
    }

    private fun buildCronWork(task: CrawlTask, config: ScheduleConfig): OneTimeWorkRequest {
        // For cron, we schedule a one-time work that reschedules itself
        // Or use a more sophisticated cron library
        val delay = calculateInitialDelay(config)
        return OneTimeWorkRequestBuilder<CrawlWorker>()
            .setInputData(workDataOf("task_id" to task.id, "cron" to config.cronExpression ?: ""))
            .setInitialDelay(delay.toLong(), TimeUnit.MILLISECONDS)
            .setConstraints(buildConstraints())
            .build()
    }

    private fun buildConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun calculateInitialDelay(config: ScheduleConfig): Long {
        val now = Instant.now()
        val target = when {
            config.timeOfDay != null -> {
                val today = LocalDateTime.now(TimeZone.currentSystemDefault())
                val targetToday = today.with(config.timeOfDay!!)
                if (targetToday > today) targetToday.toInstant(TimeZone.currentSystemDefault())
                else targetToday.plusDays(1).toInstant(TimeZone.currentSystemDefault())
            }
            config.cronExpression != null -> {
                // Parse cron and find next execution
                try {
                    val pattern = SchedulingPattern(config.cronExpression!!)
                    val next = pattern.next(java.util.Date())
                    Instant.ofEpochMillisecond(next.time)
                } catch (e: Exception) {
                    now.plusSeconds(60)
                }
            }
            else -> now.plusSeconds(60)
        }
        return (target.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0)
    }

    private fun calculateNextRun(config: ScheduleConfig): Instant? {
        return try {
            val now = Instant.now()
            when {
                config.timeOfDay != null -> {
                    val today = LocalDateTime.now(TimeZone.currentSystemDefault())
                    val targetToday = today.with(config.timeOfDay!!)
                    if (targetToday > today) targetToday.toInstant(TimeZone.currentSystemDefault())
                    else targetToday.plusDays(1).toInstant(TimeZone.currentSystemDefault())
                }
                config.cronExpression != null -> {
                    val pattern = SchedulingPattern(config.cronExpression!!)
                    val next = pattern.next(java.util.Date())
                    Instant.ofEpochMillisecond(next.time)
                }
                else -> now.plusSeconds(3600)
            }
        } catch (e: Exception) {
            null
        }
    }
}