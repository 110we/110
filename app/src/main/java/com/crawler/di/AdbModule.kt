package com.crawler.di

import android.content.Context
import com.crawler.data.adb.AdbClient
import com.crawler.data.repository.AdbRepositoryImpl
import com.crawler.domain.repository.AdbRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdbModule {

    @Provides
    @Singleton
    fun provideAdbClient(@ApplicationContext context: Context): AdbClient {
        return AdbClient(context)
    }

    @Provides
    @Singleton
    fun provideAdbRepository(adbClient: AdbClient): AdbRepository {
        return AdbRepositoryImpl(adbClient)
    }
}
