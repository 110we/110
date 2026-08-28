package com.crawler.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import com.crawler.data.entity.AppSettingsEntity
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.future.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val dataStore = RxPreferenceDataStoreBuilder(context, "settings").build()

    private val defaultUserAgentKey = preferencesKey<String>("default_user_agent")
    private val defaultTimeoutKey = preferencesKey<Long>("default_timeout")
    private val defaultMaxRedirectsKey = preferencesKey<Long>("default_max_redirects")
    private val defaultConcurrencyKey = preferencesKey<Long>("default_concurrency")
    private val globalRateLimitKey = preferencesKey<String>("global_rate_limit")
    private val robotsTxtComplianceKey = preferencesKey<Boolean>("robots_txt_compliance")
    private val jsRenderingDefaultEnabledKey = preferencesKey<Boolean>("js_rendering_default_enabled")
    private val jsRenderingDefaultTimeoutKey = preferencesKey<Long>("js_rendering_default_timeout")

    fun getSettings(): Flowable<AppSettingsEntity> {
        return dataStore.data
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

    suspend fun updateSetting(key: String, value: Any): Boolean {
        return try {
            dataStore.edit { prefs ->
                when (key) {
                    "default_user_agent" -> prefs[defaultUserAgentKey] = value as String
                    "default_timeout" -> prefs[defaultTimeoutKey] = (value as Number).toLong()
                    "default_max_redirects" -> prefs[defaultMaxRedirectsKey] = (value as Number).toLong()
                    "default_concurrency" -> prefs[defaultConcurrencyKey] = (value as Number).toLong()
                    "global_rate_limit" -> prefs[globalRateLimitKey] = value.toString()
                    "robots_txt_compliance" -> prefs[robotsTxtComplianceKey] = value as Boolean
                    "js_rendering_default_enabled" -> prefs[jsRenderingDefaultEnabledKey] = value as Boolean
                    "js_rendering_default_timeout" -> prefs[jsRenderingDefaultTimeoutKey] = (value as Number).toLong()
                }
            }.toFuture().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetDefaults(): Boolean {
        return try {
            dataStore.edit { prefs ->
                prefs[defaultUserAgentKey] = "CrawlerApp/1.0"
                prefs[defaultTimeoutKey] = 30L
                prefs[defaultMaxRedirectsKey] = 10L
                prefs[defaultConcurrencyKey] = 5L
                prefs[globalRateLimitKey] = "10.0"
                prefs[robotsTxtComplianceKey] = true
                prefs[jsRenderingDefaultEnabledKey] = false
                prefs[jsRenderingDefaultTimeoutKey] = 30L
            }.toFuture().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}