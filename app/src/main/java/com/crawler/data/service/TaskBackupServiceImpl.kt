package com.crawler.data.service

import com.crawler.data.entity.CrawlResultEntity
import com.crawler.data.entity.CrawlTaskEntity
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.data.security.ArchiveCrypto
import com.crawler.domain.service.FullBackupData
import com.crawler.domain.service.TaskBackupService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskBackupServiceImpl @Inject constructor(
    private val taskRepository: TaskRepository,
    private val resultRepository: ResultRepository
) : TaskBackupService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportTasks(): Result<String> {
        return try {
            val entities = taskRepository.exportTasks()
            val jsonStr = json.encodeToString<List<CrawlTaskEntity>>(entities)
            Result.success(jsonStr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importTasks(jsonContent: String): Result<Int> {
        return try {
            val tasks = json.decodeFromString<List<CrawlTaskEntity>>(jsonContent)
            val newIds = taskRepository.importTasks(tasks)
            Result.success(newIds.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportFullBackup(includeResults: Boolean): Result<FullBackupData> {
        return try {
            val tasks = taskRepository.exportTasks()
            val results = if (includeResults) {
                resultRepository.getTotal()
            } else {
                emptyList()
            }
            val payload = buildArchivePayload(tasks, results)
            val encrypted = ArchiveCrypto.encrypt(payload)
            Result.success(
                FullBackupData(
                    createdAt = System.currentTimeMillis(),
                    encryptedPayload = encrypted,
                    tasksCount = tasks.size,
                    resultsCount = results.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFullBackup(data: FullBackupData): Result<Int> {
        return try {
            val payload = ArchiveCrypto.decrypt(data.encryptedPayload)
            val parsed = json.parseToJsonElement(payload).jsonObject
            val tasksJson = parsed["tasks"]?.toString() ?: "[]"
            val resultsJson = parsed["results"]?.toString() ?: "[]"
            val tasks = json.decodeFromString<List<CrawlTaskEntity>>(tasksJson)
            val results = json.decodeFromString<List<CrawlResultEntity>>(resultsJson)
            val newIds = taskRepository.importTasks(tasks)
            resultRepository.insertAll(results)
            Result.success(newIds.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildArchivePayload(tasks: List<CrawlTaskEntity>, results: List<CrawlResultEntity>): String {
        val tasksJson = json.encodeToString<List<CrawlTaskEntity>>(tasks)
        val resultsJson = json.encodeToString<List<CrawlResultEntity>>(results)
        return """
            {"version":1,"createdAt":${System.currentTimeMillis()},"tasks":$tasksJson,"results":$resultsJson}
        """.trimIndent()
    }
}