package com.crawler.data.dao

import androidx.room.*
import com.crawler.data.entity.AppSettingsEntity
import com.crawler.data.entity.CrawlResultEntity
import com.crawler.data.entity.CrawlTaskEntity
import com.crawler.data.entity.ResultStatus
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: CrawlTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<CrawlTaskEntity>)

    @Update
    suspend fun update(task: CrawlTaskEntity): Int

    @Delete
    suspend fun delete(task: CrawlTaskEntity): Int

    @Query("DELETE FROM crawl_tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String): Int

    @Query("SELECT * FROM crawl_tasks WHERE id = :taskId")
    suspend fun getById(taskId: String): CrawlTaskEntity?

    @Query("SELECT * FROM crawl_tasks WHERE name = :name")
    suspend fun getByName(name: String): CrawlTaskEntity?

    @Query("SELECT * FROM crawl_tasks ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<CrawlTaskEntity>>

    @Query("SELECT * FROM crawl_tasks WHERE scheduleConfig_type IS NOT NULL AND scheduleConfig_enabled = 1")
    suspend fun getScheduledTasks(): List<CrawlTaskEntity>

    @Query("SELECT COUNT(*) FROM crawl_tasks")
    suspend fun getCount(): Int
}

@Dao
interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: CrawlResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<CrawlResultEntity>)

    @Update
    suspend fun update(result: CrawlResultEntity): Int

    @Delete
    suspend fun delete(result: CrawlResultEntity): Int

    @Query("DELETE FROM crawl_results WHERE id = :resultId")
    suspend fun deleteById(resultId: String): Int

    @Query("DELETE FROM crawl_results WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String): Int

    @Query("SELECT * FROM crawl_results WHERE id = :resultId")
    suspend fun getById(resultId: String): CrawlResultEntity?

    @Query("SELECT * FROM crawl_results WHERE taskId = :taskId ORDER BY crawledAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getByTaskIdPaged(taskId: String, limit: Int, offset: Int): List<CrawlResultEntity>

    @Query("SELECT * FROM crawl_results WHERE taskId = :taskId ORDER BY crawledAt DESC")
    fun getByTaskIdFlow(taskId: String): Flow<List<CrawlResultEntity>>

    @Query("SELECT COUNT(*) FROM crawl_results WHERE taskId = :taskId")
    suspend fun getCountByTaskId(taskId: String): Int

    @Query("SELECT COUNT(*) FROM crawl_results WHERE taskId = :taskId AND status = :status")
    suspend fun getCountByTaskIdAndStatus(taskId: String, status: ResultStatus): Int

    @Query("SELECT SUM(LENGTH(extractedData)) FROM crawl_results WHERE taskId = :taskId")
    suspend fun getTotalDataSizeByTaskId(taskId: String): Long

    @Query("SELECT SUM(LENGTH(extractedData)) FROM crawl_results")
    suspend fun getTotalDataSize(): Long

    @RawQuery(observedEntities = [CrawlResultEntity::class])
    fun getPagedResults(query: androidx.sqlite.db.SupportSQLiteQuery): PagingSource<Int, CrawlResultEntity>
}

@Dao
interface SettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettingsEntity)

    @Update
    suspend fun update(settings: AppSettingsEntity): Int
}