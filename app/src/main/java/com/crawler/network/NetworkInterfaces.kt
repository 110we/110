package com.crawler.network

import com.crawler.domain.model.RequestConfig
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.MediaType
import okhttp3.RequestBody

interface FetchStrategy {
    suspend fun fetch(url: String, config: RequestConfig): FetchResponse
}

data class FetchResponse(
    val isSuccess: Boolean,
    val statusCode: Int,
    val body: String,
    val contentType: String?,
    val errorMessage: String?,
    val finalUrl: String
) {
    companion object {
        fun success(statusCode: Int, body: String, contentType: String?, finalUrl: String): FetchResponse {
            return FetchResponse(true, statusCode, body, contentType, null, finalUrl)
        }

        fun error(statusCode: Int, errorMessage: String, finalUrl: String = ""): FetchResponse {
            return FetchResponse(false, statusCode, "", null, errorMessage, finalUrl)
        }
    }
}

interface NetworkClient {
    fun getStandardStrategy(): FetchStrategy
    fun getJsRenderStrategy(config: com.crawler.domain.model.JsRenderingConfig): FetchStrategy
    val dispatcher: CoroutineDispatcher
}