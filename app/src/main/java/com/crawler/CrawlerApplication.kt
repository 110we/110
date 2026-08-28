package com.crawler

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CrawlerApplication : Application(), Configuration.Provider {

    companion object {
        @Suppress("UNUSED_PROPERTY")
        lateinit var instance: CrawlerApplication
            private set
    }

    @Inject
    lateinit var workerFactory: WorkerFactory

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            timber.Timber.plant(timber.Timber.DebugTree())
        }
    }

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .build()
}