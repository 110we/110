package com.crawler.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crawler.data.converter.Converters
import com.crawler.data.dao.ResultDao
import com.crawler.data.dao.SettingsDao
import com.crawler.data.dao.TaskDao
import com.crawler.data.entity.CrawlResultEntity
import com.crawler.data.entity.CrawlTaskEntity
import com.crawler.data.entity.AppSettingsEntity

@Database(
    entities = [
        CrawlTaskEntity::class,
        CrawlResultEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CrawlDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun resultDao(): ResultDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: CrawlDatabase? = null

        fun getInstance(context: Context): CrawlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrawlDatabase::class.java,
                    "crawler_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}