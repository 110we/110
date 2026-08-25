package com.crawler.domain.service

import com.crawler.domain.model.*
import com.crawler.network.NetworkClient
import com.crawler.network.StandardFetchStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

interface SyncApi {
    @POST
    suspend fun syncData(
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String,
        @Body payload: Any
    ): retrofit2.Response<Any>
}

@Singleton
class SyncServiceImpl @Inject constructor(
    private val networkClient: NetworkClient
) : SyncService {

    override suspend fun sync(results: List<CrawlResult>, config: SyncConfig): SyncResult {
        if (!config.enabled || config.endpoint.isBlank()) {
            return SyncResult(false, 0, "Sync not configured")
        }

        var syncedCount = 0
        var lastError: String? = null

        for (result in results) {
            var attempt = 0
            val maxRetries = 5
            var success = false

            while (attempt < maxRetries && !success) {
                attempt++
                val syncResult = try {
                    sendToServer(result, config)
                } catch (e: Exception) {
                    lastError = e.message
                    false
                }

                if (syncResult) {
                    syncedCount++
                    success = true
                } else {
                    // Exponential backoff: 2s, 4s, 8s, 16s, 32s
                    val backoff = (2.0 * Math.pow(2.0, attempt - 1)).toLong()
                    delay(backoff * 1000)
                }
            }

            if (!success) {
                lastError = "Failed after $maxRetries retries: $lastError"
            }
        }

        return SyncResult(syncedCount == results.size, syncedCount, lastError)
    }

    private suspend fun sendToServer(result: CrawlResult, config: SyncConfig): Boolean {
        val authHeader = buildAuthHeader(config)
        val payload = buildPayload(result, config)

        val retrofit = Retrofit.Builder()
            .baseUrl(config.endpoint)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(networkClient.getStandardStrategy() as? okhttp3.OkHttpClient ?: buildOkHttpClient())
            .build()

        val api = retrofit.create(SyncApi::class.java)
        val response = api.syncData(authHeader, "application/json", payload)

        return response.isSuccessful
    }

    private fun buildAuthHeader(config: SyncConfig): String {
        return when (config.authType) {
            AuthType.BEARER -> "Bearer ${config.credentials.password}"
            AuthType.API_KEY -> "ApiKey ${config.credentials.password}"
            AuthType.BASIC -> {
                val credentials = "${config.credentials.username}:${config.credentials.password}"
                val encoded = android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
                "Basic $encoded"
            }
        }
    }

    private fun buildPayload(result: CrawlResult, config: SyncConfig): Map<String, Any> {
        return mapOf(
            "task_id" to result.taskId,
            "url" to result.url,
            "data" to result.data,
            "status" to result.status.name,
            "crawled_at" to result.crawledAt.toString(),
            "error_message" to result.errorMessage
        )
    }

    private fun buildOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
}