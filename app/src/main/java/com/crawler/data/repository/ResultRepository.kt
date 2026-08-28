package com.crawler.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.Pager
import androidx.paging.cachedIn
import com.crawler.data.dao.ResultDao
import com.crawler.data.entity.CrawlResultEntity
import com.crawler.data.entity.ResultStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultRepository @Inject constructor(
    private val resultDao: ResultDao
) {

    private val pagingConfig = PagingConfig(
        pageSize = 50,
        enablePlaceholders = false,
        prefetchDistance = 10
    )

    fun getResultsPaged(
        taskId: String,
        scope: CoroutineScope
    ): Flow<PagingData<CrawlResultEntity>> {
        return Pager(pagingConfig) {
            ResultPagingSource(resultDao, taskId)
        }.flow.cachedIn(scope)
    }

    suspend fun getByTaskId(taskId: String, limit: Int = 100, offset: Int = 0): List<CrawlResultEntity> {
        return resultDao.getByTaskIdPaged(taskId, limit, offset)
    }

    suspend fun getCountByTaskId(taskId: String): Int {
        return resultDao.getCountByTaskId(taskId)
    }

    suspend fun getCountByTaskIdAndStatus(taskId: String, status: ResultStatus): Int {
        return resultDao.getCountByTaskIdAndStatus(taskId, status)
    }

    suspend fun getTotalDataSizeByTaskId(taskId: String): Long {
        return resultDao.getTotalDataSizeByTaskId(taskId)
    }

    suspend fun getTotalDataSize(): Long {
        return resultDao.getTotalDataSize()
    }

    suspend fun deleteByTaskId(taskId: String): Int {
        return resultDao.deleteByTaskId(taskId)
    }

    suspend fun insert(result: CrawlResultEntity): Long {
        return resultDao.insert(result)
    }

    suspend fun insertAll(results: List<CrawlResultEntity>) {
        resultDao.insertAll(results)
    }

    suspend fun update(result: CrawlResultEntity): Int {
        return resultDao.update(result)
    }
}

class ResultPagingSource(
    private val resultDao: ResultDao,
    private val taskId: String
) : PagingSource<Int, CrawlResultEntity>() {

    override suspend fun load(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult<Int, CrawlResultEntity> {
        val page = params.key ?: 1
        val limit = 50
        val offset = (page - 1) * limit

        return try {
            val results = resultDao.getByTaskIdPaged(taskId, limit, offset)
            PagingSource.LoadResult.Page(
                data = results,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (results.size == limit) page + 1 else null
            )
        } catch (e: Exception) {
            PagingSource.LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CrawlResultEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1) ?: 1
        }
    }
}