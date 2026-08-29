package com.crawler.domain.service

interface TaskBackupService {

    suspend fun exportTasks(): Result<String>

    suspend fun importTasks(jsonContent: String): Result<Int>

    suspend fun exportFullBackup(includeResults: Boolean): Result<FullBackupData>

    suspend fun restoreFullBackup(data: FullBackupData): Result<Int>
}

data class FullBackupData(
    val version: Int = 1,
    val createdAt: Long,
    val encryptedPayload: String,
    val tasksCount: Int,
    val resultsCount: Int
)
