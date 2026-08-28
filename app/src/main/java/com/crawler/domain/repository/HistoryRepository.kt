package com.crawler.domain.repository

import com.crawler.data.history.CrawlHistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    fun getAll(): Flow<List<CrawlHistoryEntity>>

    fun getByTaskId(taskId: String): Flow<List<CrawlHistoryEntity>>

    suspend fun recordStart(
        taskId: String,
        taskName: String,
        triggerType: String
    ): String

    suspend fun recordCompletion(
        historyId: String,
        status: String,
        pagesCrawled: Int,
        itemsExtracted: Long,
        errors: Int
    )

    suspend fun deleteById(historyId: String): Int

    suspend fun deleteAll(): Int
}
