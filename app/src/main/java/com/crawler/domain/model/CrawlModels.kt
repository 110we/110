package com.crawler.domain.model

import android.os.Parcelable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DayOfWeek
import java.util.*
import kotlinx.parcelize.Parcelize

// Core domain models (matching design.md)

data class CrawlTask(
    val id: String,
    val name: String,
    val baseUrls: List<String>,
    val urlPatterns: UrlPatterns,
    val extractionRules: List<ExtractionRule>,
    val requestConfig: RequestConfig,
    val scheduleConfig: ScheduleConfig?,
    val jsRenderingConfig: JsRenderingConfig?,
    val syncConfig: SyncConfig?,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

data class UrlPatterns(
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = emptyList(),
    val maxDepth: Int = 3,
    val maxPages: Int = 1000
)

data class ExtractionRule(
    val fieldName: String,
    val selectorType: SelectorType,
    val expression: String,
    val attribute: String? = "text",
    val multiple: MultipleStrategy = MultipleStrategy.FIRST,
    val joinDelimiter: String = ", ",
    val postProcessors: List<PostProcessor> = emptyList()
)

enum class SelectorType { CSS, XPATH, REGEX }
enum class MultipleStrategy { FIRST, ALL_ARRAY, JOIN }

sealed interface PostProcessor {
    data class Trim(val enabled: Boolean = true) : PostProcessor
    data class RegexReplace(val pattern: String, val replacement: String) : PostProcessor
    data class TypeConversion(val targetType: DataType) : PostProcessor
    enum class DataType { STRING, INTEGER, LONG, DOUBLE, BOOLEAN, DATE }
}

data class RequestConfig(
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap(),
    val body: String? = null,
    val bodyType: BodyType = BodyType.NONE,
    val timeoutSeconds: Int = 30,
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 10,
    val userAgent: String? = null
)

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }
enum class BodyType { NONE, FORM, JSON, MULTIPART }

data class ScheduleConfig(
    val type: ScheduleType,
    val cronExpression: String? = null,
    val timeOfDay: LocalTime? = null,
    val dayOfWeek: DayOfWeek? = null,
    val dayOfMonth: Int? = null,
    val enabled: Boolean = true
)

enum class ScheduleType { ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM }

data class JsRenderingConfig(
    val enabled: Boolean = false,
    val waitCondition: WaitCondition = WaitCondition.NETWORK_IDLE,
    val waitSelector: String? = null,
    val waitScript: String? = null,
    val timeoutSeconds: Int = 30,
    val blockResources: Set<String> = setOf("image", "stylesheet", "font", "media")
)

enum class WaitCondition { NETWORK_IDLE, SELECTOR, SCRIPT, TIMEOUT }

data class SyncConfig(
    val enabled: Boolean = false,
    val endpoint: String,
    val authType: AuthType = AuthType.BEARER,
    val credentials: EncryptedCredentials,
    val payloadFormat: PayloadFormat = PayloadFormat.JSON,
    val syncOnComplete: Boolean = true
)

enum class AuthType { BEARER, API_KEY, BASIC }
enum class PayloadFormat { JSON, FORM }

data class EncryptedCredentials(
    val username: String,
    val password: String
)

// Runtime state models

@Parcelize
data class CrawlProgress(
    val taskId: String,
    val currentUrl: String?,
    val pagesCrawled: Int,
    val itemsExtracted: Long,
    val errors: Int,
    val elapsedMs: Long,
    val status: CrawlStatus
) : Parcelable

enum class CrawlStatus { IDLE, RUNNING, PAUSED, COMPLETED, FAILED, STOPPED }

data class CrawlResult(
    val id: String,
    val taskId: String,
    val url: String,
    val data: Map<String, Any>,
    val status: ResultStatus,
    val errorMessage: String?,
    val crawledAt: Instant,
    val syncedAt: Instant? = null
)

enum class ResultStatus { SUCCESS, PARTIAL, ERROR }

// Engine interfaces

interface CrawlEngine {
    suspend fun execute(task: CrawlTask, progressListener: (CrawlProgress) -> Unit): CrawlResultSummary
    suspend fun executeSingle(url: String, rules: List<ExtractionRule>): ExtractedData
}

data class CrawlResultSummary(
    val totalPages: Int,
    val totalItems: Long,
    val totalErrors: Int,
    val status: CrawlStatus
)

data class ExtractedData(
    val url: String,
    val data: Map<String, Any>,
    val status: ResultStatus,
    val errorMessage: String?
)

interface ExtractionEngine {
    fun extract(html: String, rules: List<ExtractionRule>): Map<String, Any>
    fun extractJson(json: String, rules: List<ExtractionRule>): Map<String, Any>
    fun testRule(rule: ExtractionRule, sampleHtml: String): ExtractionTestResult
}

data class ExtractionTestResult(
    val success: Boolean,
    val extractedValue: Any?,
    val errorMessage: String?
)

interface Scheduler {
    suspend fun schedule(task: CrawlTask): Result<ScheduleInfo>
    fun cancel(taskId: String)
    suspend fun reschedule(task: CrawlTask)
    suspend fun getNextRun(taskId: String): Instant?
}

data class ScheduleInfo(
    val nextRun: Instant?,
    val workId: String
)

interface ExportService {
    suspend fun exportCsv(results: List<CrawlResult>, config: ExportConfig): ExportResult
    suspend fun exportJson(results: List<CrawlResult>, config: ExportConfig): ExportResult
    suspend fun exportExcel(results: List<CrawlResult>, config: ExportConfig): ExportResult
}

data class ExportConfig(
    val format: ExportFormat,
    val includeFields: Set<String> = emptySet(),
    val filterQuery: String = "",
    val exportAll: Boolean = true,
    val excludeSensitiveFields: Boolean = false
)

enum class ExportFormat { CSV, JSON, EXCEL }

data class ExportResult(
    val uri: android.net.Uri,
    val rowCount: Int,
    val fileSize: Long,
    val filteredSensitiveCount: Int = 0
)

private val SENSITIVE_FIELD_PATTERN = Regex(
    "(?i)(password|passwd|pwd|token|secret|api[_-]?key|access[_-]?key|authorization|cookie|session|credential|private[_-]?key|auth)"
)

fun ExportConfig.resolveFields(allFields: List<String>): List<String> {
    var fields = allFields
    if (this.excludeSensitiveFields) {
        fields = fields.filterNot { SENSITIVE_FIELD_PATTERN.containsMatchIn(it) }
    }
    if (this.includeFields.isNotEmpty()) {
        fields = fields.filter { it in this.includeFields }
    }
    return fields
}

interface SyncService {
    suspend fun sync(results: List<CrawlResult>, config: SyncConfig): SyncResult
}

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val errorMessage: String?
)