package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.data.entity.toDomain
import com.crawler.data.entity.toEntity
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.CrawlTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<CrawlTask>>(emptyList())
    val tasks: StateFlow<List<CrawlTask>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val entities = taskRepository.getAll().first()
                _tasks.value = entities.map { it.toDomain() }
            } catch (e: Exception) {
                _error.value = e.message ?: "加载任务失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun createTask(task: CrawlTask): Result<Unit> {
        _error.value = null
        val existing = taskRepository.getByName(task.name)
        if (existing != null) {
            _error.value = "任务名称已存在"
            return Result.failure(Exception("任务名称已存在"))
        }

        return try {
            val entity = task.toEntity()
            taskRepository.create(entity)
            loadTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message ?: "创建失败"
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: CrawlTask): Result<Unit> {
        _error.value = null
        return try {
            val entity = task.toEntity()
            taskRepository.update(entity)
            loadTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message ?: "更新失败"
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String, deleteResults: Boolean = false): Result<Unit> {
        _error.value = null
        return try {
            taskRepository.delete(taskId, deleteResults)
            loadTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message ?: "删除失败"
            Result.failure(e)
        }
    }

    suspend fun importTasks(jsonContent: String): Result<Int> {
        _error.value = null
        return try {
            val tasks = kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.typeOf<List<com.crawler.data.entity.CrawlTaskEntity>>(), jsonContent
            )
            val newIds = taskRepository.importTasks(tasks)
            loadTasks()
            Result.success(newIds.size)
        } catch (e: Exception) {
            _error.value = "导入失败: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun exportTasks(): Result<String> {
        _error.value = null
        return try {
            val entities = taskRepository.exportTasks()
            val json = kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(
                kotlinx.serialization.typeOf<List<com.crawler.data.entity.CrawlTaskEntity>>(), entities
            )
            Result.success(json)
        } catch (e: Exception) {
            _error.value = "导出失败: ${e.message}"
            Result.failure(e)
        }
    }

    fun clearError() {
        _error.value = null
    }
}