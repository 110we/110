package com.crawler.data.repository

import com.crawler.data.dao.TaskDao
import com.crawler.data.entity.CrawlTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    suspend fun create(task: CrawlTaskEntity): Long {
        return taskDao.insert(task)
    }

    suspend fun update(task: CrawlTaskEntity): Int {
        task = task.copy(updatedAt = System.currentTimeMillis())
        return taskDao.update(task)
    }

    suspend fun delete(taskId: String, deleteResults: Boolean = false): Int {
        // Note: Results deletion should be handled by ResultRepository
        return taskDao.deleteById(taskId)
    }

    suspend fun getById(taskId: String): CrawlTaskEntity? {
        return taskDao.getById(taskId)
    }

    suspend fun getByName(name: String): CrawlTaskEntity? {
        return taskDao.getByName(name)
    }

    fun getAll(): Flow<List<CrawlTaskEntity>> {
        return taskDao.getAll()
    }

    suspend fun getScheduledTasks(): List<CrawlTaskEntity> {
        return taskDao.getScheduledTasks()
    }

    suspend fun importTasks(tasks: List<CrawlTaskEntity>): List<String> {
        val newIds = mutableListOf<String>()
        for (task in tasks) {
            val newTask = task.copy(id = java.util.UUID.randomUUID().toString())
            taskDao.insert(newTask)
            newIds.add(newTask.id)
        }
        return newIds
    }

    suspend fun exportTasks(): List<CrawlTaskEntity> {
        return taskDao.getAll().first()
    }
}