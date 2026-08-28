package com.crawler.data.repository

import com.crawler.data.history.CrawlHistoryEntity
import com.crawler.data.history.HistoryDao
import com.crawler.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAll(): Flow<List<CrawlHistoryEntity>> {
        return historyDao.getAll()
    }

    override fun getByTaskId(taskId: String): Flow<List<CrawlHistoryEntity>> {
        return historyDao.getByTaskId(taskId)
    }

    override suspend fun recordStart(
        taskId: String,
        taskName: String,
        triggerType: String
    ): String {
        val entity = CrawlHistoryEntity(
            taskId = taskId,
            taskName = taskName,
            status = CrawlHistoryEntity.STATUS_RUNNING,
            triggerType = triggerType
        )
        historyDao.insert(entity)
        return entity.id
    }

    override suspend fun recordCompletion(
        historyId: String,
        status: String,
        pagesCrawled: Int,
        itemsExtracted: Long,
        errors: Int
    ) {
        val existing = historyDao.getById(historyId) ?: return
        historyDao.insert(existing.copy(
            status = status,
            pagesCrawled = pagesCrawled,
            itemsExtracted = itemsExtracted,
            errors = errors,
            completedAt = System.currentTimeMillis()
        ))
    }

    override suspend fun deleteById(historyId: String): Int {
        return historyDao.deleteById(historyId)
    }

    override suspend fun deleteAll(): Int {
        return historyDao.deleteAll()
    }
}
