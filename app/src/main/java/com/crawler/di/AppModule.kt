package com.crawler.di

import android.content.Context
import com.crawler.data.repository.*
import com.crawler.data.security.CredentialsManager
import com.crawler.domain.engine.*
import com.crawler.domain.scheduler.Scheduler
import com.crawler.domain.scheduler.SchedulerImpl
import com.crawler.domain.service.*
import com.crawler.util.PermissionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCredentialsManager(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): CredentialsManager {
        return CredentialsManager(context)
    }

    @Provides
    @Singleton
    fun providePermissionHelper(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): PermissionHelper {
        return PermissionHelper(context)
    }

    @Provides
    @Singleton
    fun provideExtractionEngine(): ExtractionEngine {
        return ExtractionEngineImpl()
    }

    @Provides
    @Singleton
    fun provideCrawlEngine(extractionEngine: ExtractionEngine): CrawlEngine {
        return CrawlEngineImpl(extractionEngine)
    }

    @Provides
    @Singleton
    fun provideScheduler(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): Scheduler {
        return SchedulerImpl(context)
    }

    @Provides
    @Singleton
    fun provideExportService(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): ExportService {
        return ExportServiceImpl(context)
    }

    @Provides
    @Singleton
    fun provideSyncService(): SyncService {
        return SyncServiceImpl()
    }
}