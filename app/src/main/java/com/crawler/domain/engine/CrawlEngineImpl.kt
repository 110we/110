package com.crawler.domain.engine

import com.crawler.domain.model.*
import com.crawler.network.FetchStrategy
import com.crawler.network.NetworkClient
import com.crawler.network.RateLimiter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Instant
import timber.log.Timber

class CrawlEngineImpl(
    private val extractionEngine: ExtractionEngine,
    private val networkClient: NetworkClient
) : CrawlEngine {

    override suspend fun execute(
        task: CrawlTask,
        progressListener: (CrawlProgress) -> Unit
    ): CrawlResultSummary {
        val startTime = System.currentTimeMillis()
        var pagesCrawled = 0
        var itemsExtracted = 0L
        var errors = 0
        var status = CrawlStatus.RUNNING

        val semaphore = Semaphore(task.requestConfig.timeoutSeconds.coerceIn(1, 10))
        val rateLimiter = RateLimiter(task.requestConfig.timeoutSeconds.coerceIn(1, 100).toDouble())

        // For simplicity, we'll crawl base URLs only in this implementation
        // Full implementation would include URL discovery, depth tracking, etc.
        val urlsToCrawl = task.baseUrls

        for (url in urlsToCrawl) {
            // Check for cancellation
            if (status == CrawlStatus.STOPPED || status == CrawlStatus.FAILED) break

            semaphore.withPermit {
                rateLimiter.acquire()

                progressListener(CrawlProgress(
                    taskId = task.id,
                    currentUrl = url,
                    pagesCrawled = pagesCrawled,
                    itemsExtracted = itemsExtracted,
                    errors = errors,
                    elapsedMs = System.currentTimeMillis() - startTime,
                    status = CrawlStatus.RUNNING
                ))

                val result = try {
                    val fetchStrategy = getFetchStrategy(task.jsRenderingConfig)
                    val response = fetchStrategy.fetch(url, task.requestConfig)

                    if (response.isSuccess) {
                        pagesCrawled++
                        val extracted = when {
                            response.contentType?.contains("application/json") == true -> {
                                extractionEngine.extractJson(response.body, task.extractionRules)
                            }
                            else -> {
                                extractionEngine.extract(response.body, task.extractionRules)
                            }
                        }

                        if (extracted.isNotEmpty()) {
                            itemsExtracted++
                        }

                        ExtractedData(
                            url = url,
                            data = extracted,
                            status = ResultStatus.SUCCESS,
                            errorMessage = null
                        )
                    } else {
                        errors++
                        ExtractedData(
                            url = url,
                            data = emptyMap(),
                            status = ResultStatus.ERROR,
                            errorMessage = response.errorMessage
                        )
                    }
                } catch (e: Exception) {
                    errors++
                    Timber.e(e, "Error crawling $url")
                    ExtractedData(
                        url = url,
                        data = emptyMap(),
                        status = ResultStatus.ERROR,
                        errorMessage = e.message
                    )
                }

                // In a full implementation, we would persist the result here
            }
        }

        status = if (errors == urlsToCrawl.size) CrawlStatus.FAILED else CrawlStatus.COMPLETED

        return CrawlResultSummary(
            totalPages = pagesCrawled,
            totalItems = itemsExtracted,
            totalErrors = errors,
            status = status
        )
    }

    override suspend fun executeSingle(url: String, rules: List<ExtractionRule>): ExtractedData {
        val fetchStrategy = getFetchStrategy(null)
        val requestConfig = RequestConfig()

        val response = fetchStrategy.fetch(url, requestConfig)

        return if (response.isSuccess) {
            val extracted = when {
                response.contentType?.contains("application/json") == true -> {
                    extractionEngine.extractJson(response.body, rules)
                }
                else -> {
                    extractionEngine.extract(response.body, rules)
                }
            }
            ExtractedData(
                url = url,
                data = extracted,
                status = if (extracted.isNotEmpty()) ResultStatus.SUCCESS else ResultStatus.PARTIAL,
                errorMessage = if (extracted.isEmpty()) "No data extracted" else null
            )
        } else {
            ExtractedData(
                url = url,
                data = emptyMap(),
                status = ResultStatus.ERROR,
                errorMessage = response.errorMessage
            )
        }
    }

    private fun getFetchStrategy(jsConfig: JsRenderingConfig?): FetchStrategy {
        return if (jsConfig?.enabled == true) {
            networkClient.getJsRenderStrategy(jsConfig)
        } else {
            networkClient.getStandardStrategy()
        }
    }
}