package com.crawler

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CrawlerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            timber.Timber.plant(timber.Timber.DebugTree())
        }
    }
}