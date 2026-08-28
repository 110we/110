package com.crawler.network

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import com.crawler.domain.model.BodyType
import com.crawler.domain.model.HttpMethod
import com.crawler.domain.model.RequestConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import timber.log.Timber
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkClientImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : NetworkClient {

    private val okHttpClient: OkHttpClient = buildOkHttpClient()
    private val jsRenderStrategy: JsRenderStrategy = JsRenderStrategy(context)
    private val domainRateLimiters = mutableMapOf<String, RateLimiter>()
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun getStandardStrategy(): FetchStrategy {
        return StandardFetchStrategy(okHttpClient, this::getDomainRateLimiter)
    }

    override fun getJsRenderStrategy(config: com.crawler.domain.model.JsRenderingConfig): FetchStrategy {
        return jsRenderStrategy.configure(config)
    }

    private fun getDomainRateLimiter(url: String): RateLimiter {
        val domain = try { java.net.URL(url).host } catch (_: Exception) { "default" }
        return domainRateLimiters.getOrPut(domain) { RateLimiter(2.0) }
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val cookieJar = PersistentCookieJar(context)
        val defaultUserAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        val rateLimiter = RateLimiter(2.0) // 默认 2 req/s
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .cookieJar(cookieJar)
            .addNetworkInterceptor(LoggingInterceptor())
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("User-Agent", defaultUserAgent)
                    .build()
                // 请求前限流
                kotlinx.coroutines.runBlocking { rateLimiter.acquire() }
                // 随机延迟 500-2000ms 防封禁
                val delay = (500 + kotlin.random.Random.nextInt(1500)).toLong()
                kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(delay) }
                chain.proceed(request)
            }
            .build()
    }

    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            Timber.d("→ %s %s", request.method, request.url)
            val start = System.nanoTime()
            val response = chain.proceed(request)
            val elapsed = (System.nanoTime() - start) / 1_000_000
            Timber.d("← %d %s (%.2fms)", response.code, response.request.url, elapsed)
            return response
        }
    }

    private class PersistentCookieJar(private val context: Context) : CookieJar {
        private val cookieManager = CookieManager.getInstance()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isNotEmpty()) {
                val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                cookieManager.setCookie(url.toString(), cookieHeader)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookieHeader = cookieManager.getCookie(url.toString())
            return if (cookieHeader != null) {
                cookieHeader.split("; ").mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        Cookie.Builder()
                            .name(parts[0].trim())
                            .value(parts[1].trim())
                            .domain(url.host)
                            .path("/")
                            .build()
                    } else null
                }
            } else emptyList()
        }
    }
}

private suspend fun okhttp3.Call.await(): Response {
    val call = this
    val future = java.util.concurrent.CompletableFuture<Response>()
    enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            future.completeExceptionally(e)
        }

        override fun onResponse(call: okhttp3.Call, response: Response) {
            future.complete(response)
        }
    })
    return try {
        future.await()
    } catch (e: kotlinx.coroutines.CancellationException) {
        call.cancel()
        throw e
    }
}

class StandardFetchStrategy(
    private val client: OkHttpClient,
    private val getRateLimiter: (String) -> RateLimiter
) : FetchStrategy {

    override suspend fun fetch(url: String, config: RequestConfig): FetchResponse {
        val request = buildRequest(url, config)
        // 域名级限流
        getRateLimiter(url).acquire()
        return try {
            val response = client.newCall(request).await()
            val body = response.body?.string() ?: ""
            val contentType = response.header("Content-Type")
            val finalUrl = response.request.url.toString()
            if (response.isSuccessful) {
                FetchResponse.success(response.code, body, contentType, finalUrl)
            } else {
                FetchResponse.error(response.code, "HTTP ${response.code}", finalUrl)
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetch error for $url")
            FetchResponse.error(0, e.message ?: "Unknown error", url)
        }
    }

    private fun buildRequest(url: String, config: RequestConfig): Request {
        val builder = Request.Builder().url(url)

        config.headers.forEach { (k, v) -> builder.header(k, v) }
        config.cookies.forEach { (k, v) -> builder.addHeader("Cookie", "$k=$v") }

        when (config.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(createRequestBody(config)!!)
            HttpMethod.PUT -> builder.put(createRequestBody(config)!!)
            HttpMethod.PATCH -> builder.patch(createRequestBody(config)!!)
            HttpMethod.DELETE -> builder.delete(createRequestBody(config)!!)
        }

        // 仅当配置了自定义 UA 时才覆盖默认 UA
        config.userAgent?.let { builder.header("User-Agent", it) }
        return builder.build()
    }

    private fun createRequestBody(config: RequestConfig): RequestBody? {
        return when (config.bodyType) {
            BodyType.JSON -> config.body?.let {
                it.toRequestBody("application/json; charset=utf-8".toMediaType())
            }
            BodyType.FORM -> config.body?.let {
                it.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType())
            }
            BodyType.MULTIPART -> null // 需要特殊处理
            else -> null
        }
    }
}

class JsRenderStrategy(private val context: Context) : FetchStrategy {

    private var config: com.crawler.domain.model.JsRenderingConfig? = null
    private val renderSemaphore = Semaphore(2) // 限制并发渲染器数量

    fun configure(jsConfig: com.crawler.domain.model.JsRenderingConfig): JsRenderStrategy {
        this.config = jsConfig
        return this
    }

    override suspend fun fetch(url: String, requestConfig: RequestConfig): FetchResponse {
        return renderSemaphore.withPermit {
            val webView = JsRenderWebView(context)
            try {
                webView.configure(config!!, requestConfig)
                webView.loadAndWait(url)
            } finally {
                webView.destroy()
            }
        }
    }
}

class JsRenderWebView(context: Context) : WebView(context) {

    private var config: com.crawler.domain.model.JsRenderingConfig? = null
    private var requestConfig: RequestConfig? = null
    private var result: FetchResponse? = null
    private var completed = false
    private val channel = Channel<FetchResponse>(1)

    fun configure(jsConfig: com.crawler.domain.model.JsRenderingConfig, reqConfig: RequestConfig) {
        this.config = jsConfig
        this.requestConfig = reqConfig
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.userAgentString = reqConfig.userAgent ?: "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

        // 阻塞资源加载
        if (jsConfig.blockResources.contains("image")) {
            settings.blockNetworkImage = true
        }

        webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                checkCompletion()
            }

            override fun onReceivedError(
                view: android.webkit.WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                result = FetchResponse.error(0, "WebView error: ${error.description}", url ?: "")
                complete()
            }
        }

        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                Timber.d("WebView Console: ${consoleMessage.message()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }
    }

    suspend fun loadAndWait(url: String): FetchResponse {
        loadUrl(url)
        return channel.receive()
    }

    private fun checkCompletion() {
        if (completed) return
        val waitCondition = config?.waitCondition ?: com.crawler.domain.model.WaitCondition.NETWORK_IDLE

        when (waitCondition) {
            com.crawler.domain.model.WaitCondition.NETWORK_IDLE -> {
                // 简单等待一小段时间后认为网络空闲
                postDelayed({ complete() }, 2000)
            }
            com.crawler.domain.model.WaitCondition.SELECTOR -> {
                config?.waitSelector?.let { selector ->
                    evaluateJavascript("document.querySelector('$selector') !== null") { value ->
                        if (value == "true") complete()
                    }
                } ?: run { complete() }
            }
            com.crawler.domain.model.WaitCondition.SCRIPT -> {
                config?.waitScript?.let { script ->
                    evaluateJavascript(script) { value ->
                        if (value == "true") complete()
                    }
                } ?: run { complete() }
            }
            com.crawler.domain.model.WaitCondition.TIMEOUT -> {
                postDelayed({ complete() }, (config?.timeoutSeconds ?: 30) * 1000L)
            }
        }

        // 全局超时兜底
        postDelayed({ complete() }, (config?.timeoutSeconds ?: 30) * 1000L)
    }

    private fun complete() {
        if (completed) return
        completed = true
        val html = getHtml()
        result = FetchResponse.success(200, html, "text/html", url ?: "")
        channel.trySend(result!!)
    }

    private fun getHtml(): String {
        // 同步获取 HTML (在 WebView 线程)
        var html = ""
        evaluateJavascript("document.documentElement.outerHTML") { value ->
            html = value?.replace(Regex("^\"|\"$"), "")?.replace("\\\"", "\"") ?: ""
        }
        // 简单等待结果
        Thread.sleep(500)
        return html
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }
}

// Rate Limiter for per-domain rate limiting
class RateLimiter(private val maxPerSecond: Double) {
    private val permits = maxPerSecond.toInt().coerceAtLeast(1)
    private val semaphore = Semaphore(permits)
    private var lastRefill = System.currentTimeMillis()
    private var available = permits

    suspend fun acquire() {
        semaphore.withPermit {
            refill()
            if (available > 0) {
                available--
            } else {
                // Wait for next refill
                val waitTime = 1000 - (System.currentTimeMillis() - lastRefill)
                if (waitTime > 0) {
                    kotlinx.coroutines.delay(waitTime.toLong())
                }
                refill()
                available--
            }
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefill
        if (elapsed >= 1000) {
            available = permits
            lastRefill = now
        }
    }
}