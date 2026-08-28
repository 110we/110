package com.crawler.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkerFactory
import com.crawler.data.repository.*
import com.crawler.data.security.CredentialsManager
import com.crawler.data.service.TaskBackupServiceImpl
import com.crawler.domain.engine.*
import com.crawler.domain.repository.HistoryRepository
import com.crawler.domain.model.CrawlEngine
import com.crawler.domain.model.ExportService
import com.crawler.domain.model.ExtractionEngine
import com.crawler.domain.model.Scheduler
import com.crawler.domain.model.SyncService
import com.crawler.domain.scheduler.SchedulerImpl
import com.crawler.domain.service.*
import com.crawler.network.NetworkClient
import com.crawler.network.NetworkClientImpl
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
    fun provideNetworkClient(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): NetworkClient {
        return NetworkClientImpl(context)
    }

    @Provides
    @Singleton
    fun provideExtractionEngine(): ExtractionEngine {
        return ExtractionEngineImpl()
    }

    @Provides
    @Singleton
    fun provideCrawlEngine(
        extractionEngine: ExtractionEngine,
        networkClient: NetworkClient
    ): CrawlEngine {
        return CrawlEngineImpl(extractionEngine, networkClient)
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

    @Provides
    @Singleton
    fun provideTaskBackupService(
        taskRepository: com.crawler.data.repository.TaskRepository
    ): TaskBackupService {
        return TaskBackupServiceImpl(taskRepository)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: com.crawler.data.history.HistoryDao
    ): HistoryRepository {
        return com.crawler.data.repository.HistoryRepositoryImpl(historyDao)
    }

    @Provides
    @Singleton
    fun provideWorkerFactory(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): WorkerFactory {
        return HiltWorkerFactory(context)
    }
}