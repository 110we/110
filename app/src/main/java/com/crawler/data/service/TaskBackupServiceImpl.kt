package com.crawler.data.service

import com.crawler.data.entity.CrawlTaskEntity
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.service.TaskBackupService
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskBackupServiceImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : TaskBackupService {

    override suspend fun exportTasks(): Result<String> {
        return try {
            val entities = taskRepository.exportTasks()
            val json = Json { prettyPrint = true }.encodeToString<List<CrawlTaskEntity>>(entities)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importTasks(jsonContent: String): Result<Int> {
        return try {
            val tasks = Json.decodeFromString<List<CrawlTaskEntity>>(jsonContent)
            val newIds = taskRepository.importTasks(tasks)
            Result.success(newIds.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
