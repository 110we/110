# Android Crawler Application

Feature Name: android-crawler
Updated: 2026-08-25

## Description

A comprehensive Android crawler application providing configurable web scraping capabilities with manual and scheduled execution, flexible extraction rules (CSS/XPath/Regex), JavaScript rendering support, local data persistence, multi-format export, and optional server synchronization.

## Architecture

```mermaid
graph TB
    subgraph UI["UI Layer (Jetpack Compose)"]
        MainScreen[Main Screen]
        TaskListScreen[Task List Screen]
        TaskEditorScreen[Task Editor Screen]
        RuleBuilderScreen[Rule Builder Screen]
        ResultsScreen[Results Screen]
        SettingsScreen[Settings Screen]
        HistoryScreen[History Screen]
        AdbStatusScreen[ADB Status Screen]
    end

    subgraph VM["ViewModel Layer"]
        TaskViewModel[TaskViewModel]
        CrawlViewModel[CrawlViewModel]
        ResultsViewModel[ResultsViewModel]
        SettingsViewModel[SettingsViewModel]
    end

    subgraph Domain["Domain Layer"]
        TaskRepo[TaskRepository]
        CrawlEngine[CrawlEngine]
        ExtractionEngine[ExtractionEngine]
        Scheduler[Scheduler]
        ExportService[ExportService]
        SyncService[SyncService]
    end

    subgraph Data["Data Layer"]
        RoomDB[(Room Database)]
        TaskDao[TaskDao]
        ResultDao[ResultDao]
        SettingsDao[SettingsDao]
        KeyStore[Android Keystore]
        FileSystem[File System / MediaStore]
    end

    subgraph Network["Network Layer"]
        OkHttp[OkHttp Client]
        Jsoup[Jsoup HTML Parser]
        Moshi[Moshi JSON Parser]
        WebViewRenderer[Headless WebView Renderer]
    end

    subgraph Background["Background Services"]
        CrawlWorker[CrawlWorker (WorkManager)]
        ForegroundService[CrawlForegroundService]
        SyncWorker[SyncWorker]
    end

    UI --> VM
    VM --> Domain
    Domain --> Data
    Domain --> Network
    Domain --> Background
    Background --> Domain
    CrawlWorker --> CrawlEngine
    ForegroundService --> CrawlEngine
    SyncWorker --> SyncService
```

## Components and Interfaces

### UI Layer (Jetpack Compose + Material 3)

| Component | Responsibility |
|-----------|----------------|
| `MainScreen` | Bottom navigation: Tasks, Results, Settings |
| `TaskListScreen` | List tasks with status chips, FAB for new task, swipe actions |
| `TaskEditorScreen` | Form for task config: basic info, URLs, schedule, headers, JS rendering toggle |
| `RuleBuilderScreen` | Visual rule builder: field name, selector type dropdown, expression input, live preview panel |
| `ResultsScreen` | Paginated table with column selector, search, filter chips, export button |
| `SettingsScreen` | Global preferences grouped: Network, Crawling, Storage, Security, Advanced |
| `PermissionStatusScreen` | Shows grant state for ADB permissions; actions: open settings, copy adb commands, test fallback |
| `HistoryScreen` | 爬取历史列表，展示每次任务执行的历史记录 |
| `AdbStatusScreen` | 展示 Shizuku / Root / Local 三模式状态，引导授权 Shizuku |

### ViewModel Layer

| ViewModel | State / Actions |
|-----------|-----------------|
| `TaskViewModel` | `tasks: StateFlow<List<Task>>`, `create/update/deleteTask()`, `importExportTasks()` |
| `CrawlViewModel` | `crawlState: StateFlow<CrawlState>`, `startCrawl(taskId)`, `stopCrawl()`, `progress: StateFlow<CrawlProgress>` |
| `ResultsViewModel` | `results: PagingSource<Int, Result>`, `export(format, filters)`, `deleteResults(taskId)` |
| `SettingsViewModel` | `settings: StateFlow<AppSettings>`, `updateSetting(key, value)`, `resetDefaults()` |

### Domain Layer

#### `CrawlEngine`
```kotlin
interface CrawlEngine {
    suspend fun execute(task: CrawlTask, progressListener: (CrawlProgress) -> Unit): CrawlResult
    suspend fun executeSingle(url: String, rules: List<ExtractionRule>): ExtractedData
}
```
- Orchestrates HTTP fetching, parsing, extraction, and result persistence
- Manages concurrency via `CoroutineScope` with `Semaphore` for rate limiting
- Supports pluggable fetch strategies: `StandardFetchStrategy`, `JsRenderingFetchStrategy`

#### `ExtractionEngine`
```kotlin
interface ExtractionEngine {
    fun extract(html: String, rules: List<ExtractionRule>): Map<String, Any>
    fun extractJson(json: String, rules: List<ExtractionRule>): Map<String, Any>
    fun testRule(rule: ExtractionRule, sampleHtml: String): ExtractionTestResult
}
```
- Implements CSS (Jsoup), XPath (basic jsoup XPath→CSS conversion), Regex extraction
- 注：XPath 为简化实现 (`ExtractionEngineImpl.xpathToCss`)，仅支持常见路径，未引入专用 xpath 库
- Post-processors: `TrimProcessor`, `RegexReplaceProcessor`, `TypeConversionProcessor`

#### `Scheduler`
```kotlin
interface Scheduler {
    fun schedule(task: CrawlTask): Result<ScheduleInfo>
    fun cancel(taskId: String)
    fun reschedule(task: CrawlTask)
    fun getNextRun(taskId: String): Instant?
}
```
- Wrapper around `WorkManager` with `PeriodicWorkRequest` / `OneTimeWorkRequest`
- Handles cron expressions via `com.cronutils` (cron-utils 9.2.1) for custom schedules

#### `ExportService`
```kotlin
interface ExportService {
    suspend fun exportCsv(results: List<CrawlResult>, config: ExportConfig): Uri
    suspend fun exportJson(results: List<CrawlResult>, config: ExportConfig): Uri
    suspend fun exportExcel(results: List<CrawlResult>, config: ExportConfig): Uri
}
```
- Streaming writers for large datasets: `CsvWriter`, `JsonStreamWriter`, `Apache POI` for XLSX
- Uses `MediaStore` API for Android 10+ scoped storage compliance

#### `SyncService`
```kotlin
interface SyncService {
    suspend fun sync(results: List<CrawlResult>, config: SyncConfig): SyncResult
}
```
- Retrofit-based HTTP client with interceptors for auth
- Exponential backoff retry logic

#### `TaskBackupService` (Req 10, Task Import/Export)
```kotlin
interface TaskBackupService {
    suspend fun exportTasks(): Result<String>       // 任务配置导出为 JSON（不含结果）
    suspend fun importTasks(jsonContent: String): Result<Int>  // 导入，分配新 UUID
}
```
- 基于 kotlinx-serialization 反射模式序列化任务配置
- 用于任务在设备间迁移与配置备份

### Data Layer

#### Room Database Schema

```kotlin
@Entity(tableName = "crawl_tasks")
data class CrawlTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrls: List<String>, // JSON array
    val urlPatterns: UrlPatternsEntity, // embedded
    val extractionRules: List<ExtractionRuleEntity>, // JSON
    val requestConfig: RequestConfigEntity, // embedded
    val scheduleConfig: ScheduleConfigEntity?, // embedded, nullable
    val jsRenderingConfig: JsRenderingConfigEntity?, // embedded
    val syncConfig: SyncConfigEntity?, // embedded
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "crawl_results")
data class CrawlResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val url: String,
    val extractedData: String, // JSON
    val status: ResultStatus, // SUCCESS, PARTIAL, ERROR
    val errorMessage: String?,
    val crawledAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

@TypeConverters(Converters::class)
```

#### 新增：爬取历史

```kotlin
@Entity(tableName = "crawl_history")
data class CrawlHistoryEntity(@PrimaryKey val id: String, /* ... */)
```

- 记录每次爬取的执行历史，供 `HistoryScreen` 展示

#### Security
- `CredentialsManager`: Encrypt/decrypt sensitive fields using `MasterKey` + `EncryptedSharedPreferences` (API 23+) or `Security` library
- Keys stored in Android Keystore (hardware-backed when available)

### Network Layer

| Component | Configuration |
|-----------|---------------|
| `OkHttpClient` | Connection pool: 10, timeout: 30s, retry: 3, follower: 10 redirects |
| `Jsoup` | Parser: HTML5, pretty print disabled, base URI tracking |
| `Moshi` | Kotlin adapter factory, lenient mode for malformed JSON |
| `WebViewRenderer` | Headless `WebView` in offscreen mode, `WebViewClient` for navigation callbacks, `WebChromeClient` for console messages |

### Background Services

| Service | Purpose |
|---------|---------|
| `CrawlWorker` | WorkManager worker for scheduled crawls; `setRequiredNetworkType(CONNECTED)`, `setRequiresCharging(false)` |
| `CrawlForegroundService` | Foreground service for manual crawls; persistent notification with progress, `startForeground()` with `FOREGROUND_SERVICE_DATA_SYNC` |
| `SyncWorker` | WorkManager worker for server sync; retries with `BackoffPolicy.EXPONENTIAL` |

### Permission Strategy (Open Source / Sideload Distribution)

| Feature | Primary (ADB Permission) | Fallback (Standard API) |
|---------|--------------------------|-------------------------|
| File export to arbitrary path | `MANAGE_EXTERNAL_STORAGE` + `FileOutputStream` | `MediaStore` + `ACTION_OPEN_DOCUMENT_TREE` (SAF) |
| Detect installed browsers/proxy apps | `QUERY_ALL_PACKAGES` + `PackageManager.queryIntentActivities()` | `<queries>` in Manifest + explicit intent |
| Install plugin APKs / self-update | `REQUEST_INSTALL_PACKAGES` + `INSTALL_PACKAGES` (ADB) | `ACTION_INSTALL_PACKAGE` + user confirmation |
| App usage stats for smart scheduling | `PACKAGE_USAGE_STATS` (ADB grant) | Disabled gracefully |

**Manifest declarations** (for sideload builds):
```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

**PermissionHelper component**:
```kotlin
object PermissionHelper {
    fun checkManageStorage(context: Context): Boolean =
        Environment.isExternalStorageManager()

    fun checkQueryAllPackages(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_QUERY_ALL_PACKAGES)

    fun openManageStorageSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .apply { data = Uri.parse("package:${context.packageName}") }
        context.startActivity(intent)
    }

    fun getAdbGrantCommands(packageName: String): List<String> = listOf(
        "adb shell pm grant $packageName android.permission.MANAGE_EXTERNAL_STORAGE",
        "adb shell pm grant $packageName android.permission.QUERY_ALL_PACKAGES",
        "adb shell pm grant $packageName android.permission.PACKAGE_USAGE_STATS",
        "adb shell pm grant $packageName android.permission.REQUEST_INSTALL_PACKAGES"
    )
}
```

UI 在首次使用相关功能时检测权限，缺失则弹窗提供：
- “去设置开启”按钮（跳转系统设置页）
- “复制 ADB 命令”按钮（一键复制上述命令，适合连电脑的用户）
- “使用标准模式”按钮（走 MediaStore/SAF 等回退方案）

**新增：ADB 能力（Shizuku / Root）**
- 基于 `dev.rikka.shizuku:api/provider` 集成 Shizuku，实现 shell 级访问受保护数据目录
- `AdbClient` 支持三种执行模式：`SHIZUKU`（优先）、`ROOT`（Root 环境回退）、`LOCAL`（常规权限）
- Manifest 声明 `moe.shizuku.manager.permission.API`/`API_V23`、`rikka.shizuku.ShizukuProvider`（authorities = `${applicationId}.shizuku`）
- `AdbStatusScreen` 展示三模式状态并引导：检测 Shizuku 已安装 → 启动服务 → 授权
- Shizuku 包名 `moe.shizuku.privileged.api`；`CrawlWorker` 在执行前检查 `isShizukuAuthorized || isRootAvailable`

## Data Models

### Core Entities

```kotlin
// Task Configuration
data class CrawlTask(
    val id: String,
    val name: String,
    val baseUrls: List<String>,
    val urlPatterns: UrlPatterns,
    val extractionRules: List<ExtractionRule>,
    val requestConfig: RequestConfig,
    val scheduleConfig: ScheduleConfig?,
    val jsRenderingConfig: JsRenderingConfig?,
    val syncConfig: SyncConfig?
)

data class UrlPatterns(
    val includePatterns: List<String> = emptyList(), // regex
    val excludePatterns: List<String> = emptyList(),
    val maxDepth: Int = 3,
    val maxPages: Int = 1000
)

data class ExtractionRule(
    val fieldName: String,
    val selectorType: SelectorType, // CSS, XPATH, REGEX
    val expression: String,
    val attribute: String? = "text", // text, href, src, html, or custom attr
    val multiple: MultipleStrategy = MultipleStrategy.FIRST, // FIRST, ALL_ARRAY, JOIN
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
    val body: RequestBody? = null,
    val timeoutSeconds: Int = 30,
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 10,
    val userAgent: String? = null
)

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

data class ScheduleConfig(
    val type: ScheduleType,
    val cronExpression: String? = null, // for CUSTOM
    val timeOfDay: LocalTime? = null, // for DAILY/WEEKLY/MONTHLY
    val dayOfWeek: DayOfWeek? = null, // for WEEKLY
    val dayOfMonth: Int? = null, // for MONTHLY
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

// Runtime State
data class CrawlProgress(
    val taskId: String,
    val currentUrl: String?,
    val pagesCrawled: Int,
    val itemsExtracted: Long,
    val errors: Int,
    val elapsedMs: Long,
    val status: CrawlStatus // RUNNING, PAUSED, COMPLETED, FAILED, STOPPED
)

enum class CrawlStatus { IDLE, RUNNING, PAUSED, COMPLETED, FAILED, STOPPED }

data class CrawlResult(
    val id: String,
    val taskId: String,
    val url: String,
    val data: Map<String, Any>,
    val status: ResultStatus,
    val errorMessage: String?,
    val crawledAt: Instant
)

enum class ResultStatus { SUCCESS, PARTIAL, ERROR }
```

## Correctness Properties

### Invariants

1. **Task Uniqueness**: `CrawlTask.id` is globally unique (UUID v4)
2. **Result Ownership**: Every `CrawlResultEntity.taskId` references an existing `CrawlTaskEntity`
3. **Schedule Consistency**: A task with `ScheduleConfig.enabled = true` must have a corresponding `WorkManager` job
4. **Extraction Determinism**: Given same HTML and rules, `ExtractionEngine.extract()` produces identical output
5. **Export Completeness**: Exported row count matches filtered result count exactly

### Constraints

| Constraint | Limit | Handling |
|------------|-------|----------|
| Max concurrent requests per task | 10 | Semaphore in `CrawlEngine` |
| Max concurrent JS renderers | 2 | Fixed thread pool executor |
| Max database size | 500 MB | Prompt user to archive/delete |
| Max export rows per file | 100,000 | Auto-split into multiple files |
| Max crawl depth | 10 | Configurable, default 3 |
| Request timeout | 300s | Configurable, default 30s |
| JS rendering timeout | 120s | Configurable, default 30s |
| Scheduled job minimum interval | 15 min | WorkManager constraint |

## Error Handling

| Scenario | Detection | Handling |
|----------|-----------|----------|
| Network timeout | OkHttp `SocketTimeoutException` | Retry 3x with 2s/4s/8s backoff; mark URL as error |
| HTTP 4xx/5xx | Response code check | Retry 3x for 5xx; no retry for 4xx (except 429); record error |
| DNS failure | `UnknownHostException` | Retry 3x; if persistent, mark task as failed |
| SSL/TLS error | `SSLHandshakeException` | Option to trust all (dev mode) or fail; log security warning |
| JS rendering OOM | `OutOfMemoryError` in WebView | Kill renderer process; fallback to raw HTML; reduce concurrency |
| Database full | `SQLiteFullException` | Pause crawls; notify user; offer cleanup |
| Export OOM | `OutOfMemoryError` during write | Switch to streaming writer; if still fails, split export |
| Sync auth failure | HTTP 401/403 | Notify user to update credentials; queue for retry |
| WorkManager job lost | Job not found on reschedule | Re-schedule on app startup via `TaskRepository.initSchedules()` |

### Error Propagation

```kotlin
sealed class CrawlError(val message: String, val recoverable: Boolean) {
    data class NetworkError(cause: Throwable) : CrawlError("Network error: ${cause.message}", true)
    data class HttpError(code: Int, body: String?) : CrawlError("HTTP $code", code in 500..599)
    data class ParseError(cause: Throwable) : CrawlError("Parse error: ${cause.message}", false)
    data class ExtractionError(rule: String, cause: Throwable) : CrawlError("Extraction failed for $rule", false)
    data class StorageError(cause: Throwable) : CrawlError("Storage error: ${cause.message}", true)
    data class SyncError(cause: Throwable) : CrawlError("Sync failed: ${cause.message}", true)
    object Cancelled : CrawlError("Cancelled by user", false)
    object PermissionDenied : CrawlError("Permission denied", false)
}
```

## Test Strategy

### Unit Tests (JUnit 5 + MockK)

| Module | Coverage Target | Key Scenarios |
|--------|----------------|---------------|
| `ExtractionEngine` | 90% | CSS/XPath/Regex extraction, multiple strategies, post-processors, edge cases (empty, malformed HTML) |
| `CrawlEngine` | 80% | Concurrency control, rate limiting, redirect handling, retry logic, JS rendering fallback |
| `Scheduler` | 85% | Cron parsing, WorkManager enqueue/cancel, constraint handling, timezone |
| `ExportService` | 85% | CSV RFC 4180 compliance, JSON streaming, XLSX types, large dataset streaming |
| `SyncService` | 80% | Auth interceptors, retry backoff, payload serialization |
| `CredentialsManager` | 95% | Encrypt/decrypt round-trip, key rotation, hardware vs software keystore |

### Integration Tests (AndroidX Test + Robolectric)

- Room DAO operations with in-memory database
- WorkManager scheduling with `TestWorkerBuilder`
- Foreground service lifecycle with `ServiceController`
- MediaStore export on API 29+ (scoped storage)

### Instrumented Tests (Android Device/Emulator)

| Scenario | Tool |
|----------|------|
| Full crawl flow: create task → start → verify results → export | Espresso + Compose UI tests |
| Scheduled crawl triggers at correct time | WorkManager test utilities + time acceleration |
| JS rendering on real SPA site | Physical device test with test server |
| Large dataset (10k+ results) export performance | Benchmark + memory profiling |
| Background crawl with battery saver on | Device config + WorkManager constraints |

### Test Data

- **Fixtures**: Sample HTML pages (static, dynamic, malformed), JSON APIs, XML feeds
- **Mock Server**: `MockWebServer` for controlled HTTP responses
- **Test Sites**: Local test server with known structure for E2E validation

## References

[^1]: (Android Developers) - [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
[^2]: (Android Developers) - [Foreground Services](https://developer.android.com/guide/components/foreground-services)
[^3]: (Jsoup) - [Jsoup Cookbook](https://jsoup.org/cookbook/)
[^4]: (Square) - [OkHttp Recipes](https://square.github.io/okhttp/recipes/)
[^5]: (Moshi) - [Moshi GitHub](https://github.com/square/moshi)
[^6]: (Apache POI) - [Apache POI Spreadsheet](https://poi.apache.org/components/spreadsheet/)
[^7]: (cron-utils) - [cron-utils GitHub](https://github.com/jmrozanec/cron-utils)
[^8]: (Android Security) - [Keystore System](https://developer.android.com/training/articles/keystore)
[^9]: (Kotlin Coroutines) - [Retry Library](https://github.com/aakira/kotlin-coroutines-retry)