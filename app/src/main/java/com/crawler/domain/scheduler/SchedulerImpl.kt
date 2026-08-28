package com.crawler.domain.scheduler

import android.content.Context
import androidx.work.*
import com.crawler.background.CrawlWorker
import com.crawler.domain.model.*
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.plus
import kotlinx.datetime.with
import java.time.ZonedDateTime
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
            workManager.enqueueUniqueWork(workId, ExistingWorkPolicy.REPLACE, workRequest)
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
        return null
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
        val now = Clock.System.now()
        val target = when {
            config.timeOfDay != null -> {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val targetToday = today.with(config.timeOfDay!!)
                if (targetToday > today) targetToday.toInstant(TimeZone.currentSystemDefault())
                else targetToday.plusDays(1).toInstant(TimeZone.currentSystemDefault())
            }
            config.cronExpression != null -> {
                // Parse cron and find next execution
                try {
                    val cron = CronParser(
                        CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
                    ).parse(config.cronExpression!!)
                    val next = cron.nextExecutionAfter(ZonedDateTime.now()).orElse(null)
                    if (next != null) Instant.fromEpochMilliseconds(next.toInstant().toEpochMilli())
                    else now.plus(60, DateTimeUnit.SECOND)
                } catch (e: Exception) {
                    now.plus(60, DateTimeUnit.SECOND)
                }
            }
            else -> now.plus(60, DateTimeUnit.SECOND)
        }
        return (target.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0)
    }

    private fun calculateNextRun(config: ScheduleConfig): Instant? {
        return try {
            val now = Clock.System.now()
            when {
                config.timeOfDay != null -> {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val targetToday = today.with(config.timeOfDay!!)
                    if (targetToday > today) targetToday.toInstant(TimeZone.currentSystemDefault())
                    else targetToday.plusDays(1).toInstant(TimeZone.currentSystemDefault())
                }
                config.cronExpression != null -> {
                    val cron = CronParser(
                        CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
                    ).parse(config.cronExpression!!)
                    val next = cron.nextExecutionAfter(ZonedDateTime.now()).orElse(null)
                    if (next != null) Instant.fromEpochMilliseconds(next.toInstant().toEpochMilli())
                    else now.plus(3600, DateTimeUnit.SECOND)
                }
                else -> now.plus(3600, DateTimeUnit.SECOND)
            }
        } catch (e: Exception) {
            null
        }
    }
}