package com.crawler.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.crawler.data.converter.Converters
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DayOfWeek
import java.util.*

@Entity(tableName = "crawl_tasks")
@TypeConverters(Converters::class)
data class CrawlTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrls: List<String>,
    @Embedded(prefix = "urlPatterns_") val urlPatterns: UrlPatternsEntity,
    val extractionRules: List<ExtractionRuleEntity>,
    @Embedded(prefix = "requestConfig_") val requestConfig: RequestConfigEntity,
    @Embedded(prefix = "scheduleConfig_") val scheduleConfig: ScheduleConfigEntity?,
    @Embedded(prefix = "jsRenderingConfig_") val jsRenderingConfig: JsRenderingConfigEntity?,
    @Embedded(prefix = "syncConfig_") val syncConfig: SyncConfigEntity?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "crawl_results")
@TypeConverters(Converters::class)
data class CrawlResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val url: String,
    val extractedData: String,
    val status: ResultStatus,
    val errorMessage: String?,
    val crawledAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

enum class ResultStatus {
    SUCCESS, PARTIAL, ERROR
}

@Embedded
data class UrlPatternsEntity(
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = emptyList(),
    val maxDepth: Int = 3,
    val maxPages: Int = 1000
)

data class ExtractionRuleEntity(
    val fieldName: String,
    val selectorType: SelectorType,
    val expression: String,
    val attribute: String? = "text",
    val multiple: MultipleStrategy = MultipleStrategy.FIRST,
    val joinDelimiter: String = ", ",
    val postProcessors: List<PostProcessorEntity> = emptyList()
)

enum class SelectorType {
    CSS, XPATH, REGEX
}

enum class MultipleStrategy {
    FIRST, ALL_ARRAY, JOIN
}

sealed interface PostProcessorEntity {
    data class Trim(val enabled: Boolean = true) : PostProcessorEntity
    data class RegexReplace(val pattern: String, val replacement: String) : PostProcessorEntity
    data class TypeConversion(val targetType: DataType) : PostProcessorEntity
    enum class DataType { STRING, INTEGER, LONG, DOUBLE, BOOLEAN, DATE }
}

@Embedded
data class RequestConfigEntity(
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

enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE
}

enum class BodyType {
    NONE, FORM, JSON, MULTIPART
}

@Embedded
data class ScheduleConfigEntity(
    val type: ScheduleType,
    val cronExpression: String? = null,
    val timeOfDay: LocalTime? = null,
    val dayOfWeek: DayOfWeek? = null,
    val dayOfMonth: Int? = null,
    val enabled: Boolean = true
)

enum class ScheduleType {
    ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM
}

@Embedded
data class JsRenderingConfigEntity(
    val enabled: Boolean = false,
    val waitCondition: WaitCondition = WaitCondition.NETWORK_IDLE,
    val waitSelector: String? = null,
    val waitScript: String? = null,
    val timeoutSeconds: Int = 30,
    val blockResources: Set<String> = setOf("image", "stylesheet", "font", "media")
)

enum class WaitCondition {
    NETWORK_IDLE, SELECTOR, SCRIPT, TIMEOUT
}

@Embedded
data class SyncConfigEntity(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val authType: AuthType = AuthType.BEARER,
    @Embedded(prefix = "credentials_") val credentials: EncryptedCredentialsEntity = EncryptedCredentialsEntity("", ""),
    val payloadFormat: PayloadFormat = PayloadFormat.JSON,
    val syncOnComplete: Boolean = true
)

enum class AuthType {
    BEARER, API_KEY, BASIC
}

enum class PayloadFormat {
    JSON, FORM
}

data class EncryptedCredentialsEntity(
    val username: String,
    val password: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultUserAgent: String = "CrawlerApp/1.0",
    val defaultTimeoutSeconds: Int = 30,
    val defaultMaxRedirects: Int = 10,
    val defaultConcurrency: Int = 5,
    val globalRateLimitPerSecond: Double = 10.0,
    val robotsTxtCompliance: Boolean = true,
    val jsRenderingDefaultEnabled: Boolean = false,
    val jsRenderingDefaultTimeout: Int = 30
)