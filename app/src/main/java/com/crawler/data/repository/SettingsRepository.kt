package com.crawler.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import com.crawler.data.entity.AppSettingsEntity
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val dataStore = RxPreferenceDataStoreBuilder(context, "settings").build()

    private val defaultUserAgentKey = stringPreferencesKey("default_user_agent")
    private val defaultTimeoutKey = longPreferencesKey("default_timeout")
    private val defaultMaxRedirectsKey = longPreferencesKey("default_max_redirects")
    private val defaultConcurrencyKey = longPreferencesKey("default_concurrency")
    private val globalRateLimitKey = stringPreferencesKey("global_rate_limit")
    private val robotsTxtComplianceKey = booleanPreferencesKey("robots_txt_compliance")
    private val jsRenderingDefaultEnabledKey = booleanPreferencesKey("js_rendering_default_enabled")
    private val jsRenderingDefaultTimeoutKey = longPreferencesKey("js_rendering_default_timeout")

    fun getSettings(): Flowable<AppSettingsEntity> {
        return dataStore.data()
            .map { prefs ->
                AppSettingsEntity(
                    defaultUserAgent = prefs[defaultUserAgentKey] ?: "CrawlerApp/1.0",
                    defaultTimeoutSeconds = (prefs[defaultTimeoutKey] ?: 30L).toInt(),
                    defaultMaxRedirects = (prefs[defaultMaxRedirectsKey] ?: 10L).toInt(),
                    defaultConcurrency = (prefs[defaultConcurrencyKey] ?: 5L).toInt(),
                    globalRateLimitPerSecond = (prefs[globalRateLimitKey] ?: "10.0").toDouble(),
                    robotsTxtCompliance = prefs[robotsTxtComplianceKey] ?: true,
                    jsRenderingDefaultEnabled = prefs[jsRenderingDefaultEnabledKey] ?: false,
                    jsRenderingDefaultTimeout = (prefs[jsRenderingDefaultTimeoutKey] ?: 30L).toInt()
                )
            }
            .onErrorReturn { AppSettingsEntity() }
    }

    suspend fun updateSetting(key: String, value: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            dataStore.updateDataAsync { prefs ->
                Single.fromCallable<Preferences> {
                    prefs.toMutablePreferences().apply {
                        when (key) {
                            "default_user_agent" -> this[defaultUserAgentKey] = value as String
                            "default_timeout" -> this[defaultTimeoutKey] = (value as Number).toLong()
                            "default_max_redirects" -> this[defaultMaxRedirectsKey] = (value as Number).toLong()
                            "default_concurrency" -> this[defaultConcurrencyKey] = (value as Number).toLong()
                            "global_rate_limit" -> this[globalRateLimitKey] = value.toString()
                            "robots_txt_compliance" -> this[robotsTxtComplianceKey] = value as Boolean
                            "js_rendering_default_enabled" -> this[jsRenderingDefaultEnabledKey] = value as Boolean
                            "js_rendering_default_timeout" -> this[jsRenderingDefaultTimeoutKey] = (value as Number).toLong()
                        }
                    }
                }
            }.blockingGet()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetDefaults(): Boolean = withContext(Dispatchers.IO) {
        try {
            dataStore.updateDataAsync { prefs ->
                Single.fromCallable<Preferences> {
                    prefs.toMutablePreferences().apply {
                        this[defaultUserAgentKey] = "CrawlerApp/1.0"
                        this[defaultTimeoutKey] = 30L
                        this[defaultMaxRedirectsKey] = 10L
                        this[defaultConcurrencyKey] = 5L
                        this[globalRateLimitKey] = "10.0"
                        this[robotsTxtComplianceKey] = true
                        this[jsRenderingDefaultEnabledKey] = false
                        this[jsRenderingDefaultTimeoutKey] = 30L
                    }
                }
            }.blockingGet()
            true
        } catch (e: Exception) {
            false
        }
    }
}