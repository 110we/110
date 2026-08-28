package com.crawler.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "crawl_history")
data class CrawlHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val taskName: String,
    val status: String,
    val pagesCrawled: Int = 0,
    val itemsExtracted: Long = 0L,
    val errors: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val triggerType: String = "MANUAL"
) {
    companion object {
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_STOPPED = "STOPPED"
    }
}
