package com.crawler.domain.service

interface TaskBackupService {

    suspend fun exportTasks(): Result<String>

    suspend fun importTasks(jsonContent: String): Result<Int>
}
