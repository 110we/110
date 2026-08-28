package com.crawler.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: CrawlHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<CrawlHistoryEntity>)

    @Delete
    suspend fun delete(history: CrawlHistoryEntity): Int

    @Query("DELETE FROM crawl_history WHERE id = :historyId")
    suspend fun deleteById(historyId: String): Int

    @Query("DELETE FROM crawl_history")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM crawl_history ORDER BY startedAt DESC")
    fun getAll(): Flow<List<CrawlHistoryEntity>>

    @Query("SELECT * FROM crawl_history WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun getByTaskId(taskId: String): Flow<List<CrawlHistoryEntity>>

    @Query("SELECT * FROM crawl_history WHERE id = :historyId")
    suspend fun getById(historyId: String): CrawlHistoryEntity?

    @Query("SELECT COUNT(*) FROM crawl_history")
    suspend fun getCount(): Int
}
