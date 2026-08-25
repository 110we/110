package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.LoadParams
import androidx.paging.LoadResult
import androidx.paging.cachedIn
import androidx.paging.flow.PagingData
import androidx.paging.flow.pager
import com.crawler.data.repository.ResultRepository
import com.crawler.data.repository.TaskRepository
import com.crawler.domain.model.CrawlResult
import com.crawler.domain.model.ExportConfig
import com.crawler.domain.model.ExportFormat
import com.crawler.domain.model.ExportResult
import com.crawler.domain.service.ExportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val resultRepository: ResultRepository,
    private val taskRepository: TaskRepository,
    private val exportService: ExportService
) : ViewModel() {

    private val pagingConfig = PagingConfig(
        pageSize = 50,
        enablePlaceholders = false,
        prefetchDistance = 10
    )

    private val _currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskId: StateFlow<String?> = _currentTaskId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFields = MutableStateFlow<Set<String>>(emptySet())
    val selectedFields: StateFlow<Set<String>> = _selectedFields

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setTaskId(taskId: String?) {
        _currentTaskId.value = taskId
        _searchQuery.value = ""
        _selectedFields.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleField(field: String) {
        val current = _selectedFields.value.toMutableSet()
        if (field in current) current.remove(field) else current.add(field)
        _selectedFields.value = current
    }

    fun selectAllFields(fields: List<String>) {
        _selectedFields.value = fields.toSet()
    }

    fun clearSelection() {
        _selectedFields.value = emptySet()
    }

    fun getResults(scope: CoroutineScope): androidx.paging.Flow<PagingData<CrawlResult>>? {
        val taskId = _currentTaskId.value ?: return null
        return Pager(pagingConfig) {
            ResultsPagingSource(resultRepository, taskId, _searchQuery)
        }.flow.cachedIn(scope)
    }

    suspend fun export(
        format: ExportFormat,
        exportAll: Boolean = true,
        scope: CoroutineScope
    ): Result<ExportResult> {
        val taskId = _currentTaskId.value ?: return Result.failure(Exception("未选择任务"))
        _isExporting.value = true
        _error.value = null

        return try {
            val results = if (exportAll) {
                resultRepository.getByTaskId(taskId, limit = 100000)
            } else {
                resultRepository.getByTaskId(taskId, limit = 10000)
            }

            val domainResults = results.map { it.toDomain() }
            val config = ExportConfig(
                format = format,
                includeFields = _selectedFields.value,
                filterQuery = _searchQuery.value,
                exportAll = exportAll
            )

            val result = when (format) {
                ExportFormat.CSV -> exportService.exportCsv(domainResults, config)
                ExportFormat.JSON -> exportService.exportJson(domainResults, config)
                ExportFormat.EXCEL -> exportService.exportExcel(domainResults, config)
            }

            _exportResult.value = result
            Result.success(result)
        } catch (e: Exception) {
            _error.value = e.message ?: "导出失败"
            Result.failure(e)
        } finally {
            _isExporting.value = false
        }
    }

    suspend fun deleteResults(taskId: String): Result<Unit> {
        _error.value = null
        return try {
            resultRepository.deleteByTaskId(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = e.message ?: "删除失败"
            Result.failure(e)
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// PagingSource with reactive search query
class ResultsPagingSource(
    private val resultRepository: ResultRepository,
    private val taskId: String,
    private val searchQuery: StateFlow<String>
) : PagingSource<Int, CrawlResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CrawlResult> {
        val page = params.key ?: 1
        val limit = config.pageSize
        val offset = (page - 1) * limit
        val query = searchQuery.value

        return try {
            var results = resultRepository.getByTaskId(taskId, limit, offset)
            
            if (query.isNotBlank()) {
                results = results.filter { result ->
                    result.data.values.any { it.toString().lowercase().contains(query.lowercase()) }
                        || result.url.lowercase().contains(query.lowercase())
                }
            }

            LoadResult.Page(
                data = results.map { it.toDomain() },
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (results.size == limit) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CrawlResult>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1) ?: 1
        }
    }
}