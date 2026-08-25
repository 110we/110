# Requirements Document

## Introduction

This document specifies the requirements for an Android crawler application (APK) that provides a comprehensive, configurable web crawling solution. The application supports both manual and scheduled crawling, flexible rule configuration, multiple data export options, and local data persistence.

## Glossary

- **Crawl Task**: A configured crawling job containing target URLs, extraction rules, and scheduling settings
- **Extraction Rule**: A selector-based rule (CSS/XPath/Regex) defining how to extract data from HTML/JSON responses
- **Crawl Result**: The structured data extracted from a single URL during a crawl execution
- **Export Format**: Supported output formats: CSV, JSON, Excel (XLSX)
- **WorkManager**: Android's recommended API for deferrable, guaranteed background work

## Requirements

### Requirement 1: Crawl Task Management

**User Story:** AS a user, I want to create, edit, and manage crawl tasks, so that I can define what and how to crawl.

#### Acceptance Criteria

1. WHEN the user creates a new crawl task, the system SHALL allow configuring: task name, base URL(s), URL patterns (include/exclude), extraction rules, request headers, and scheduling options
2. WHEN the user edits an existing crawl task, the system SHALL preserve previous configurations and allow modification of all fields
3. WHEN the user deletes a crawl task, the system SHALL remove the task and optionally its associated crawl results
4. WHILE viewing the task list, the system SHALL display task name, status (idle/running/completed/failed), last run time, next scheduled run, and result count
5. IF a task name is duplicated, the system SHALL reject the creation and display an error message

### Requirement 2: Flexible Extraction Rules

**User Story:** AS a user, I want to define flexible extraction rules using multiple selector types, so that I can extract data from various website structures.

#### Acceptance Criteria

1. WHEN configuring extraction rules, the system SHALL support CSS selectors, XPath expressions, and Regular Expressions
2. WHEN defining a rule, the system SHALL allow specifying: field name, selector type, selector expression, attribute to extract (text, href, src, custom attribute), and post-processing (trim, regex replace, type conversion)
3. WHEN the user tests an extraction rule against a sample URL, the system SHALL fetch the page and display extracted values in real-time
4. IF a selector returns multiple matches, the system SHALL allow choosing: first match, all matches as array, or join with delimiter
5. WHILE editing rules, the system SHALL provide a visual rule builder with live preview

### Requirement 3: Manual Crawl Execution

**User Story:** AS a user, I want to manually start and stop crawl tasks, so that I can control when crawling happens.

#### Acceptance Criteria

1. WHEN the user taps "Start Crawl" on a task, the system SHALL initiate the crawl in a foreground service with a persistent notification showing progress
2. WHILE a crawl is running, the system SHALL display real-time progress: current URL, pages crawled, items extracted, errors encountered, and elapsed time
3. WHEN the user taps "Stop Crawl", the system SHALL gracefully terminate the ongoing crawl and save partial results
4. IF the app is backgrounded during crawling, the system SHALL continue crawling via foreground service until completion or user stop
5. WHEN a crawl completes, the system SHALL show a summary notification with result count and any errors

### Requirement 4: Scheduled Background Crawling

**User Story:** AS a user, I want crawl tasks to run automatically on a schedule, so that data is collected without manual intervention.

#### Acceptance Criteria

1. WHEN configuring a task, the system SHALL allow setting schedule: one-time, daily, weekly, monthly, or custom cron expression
2. WHEN a scheduled time arrives, the system SHALL enqueue the crawl via WorkManager with network and charging constraints
3. WHILE a scheduled crawl runs, the system SHALL NOT show a persistent notification (silent background execution)
4. IF a scheduled crawl fails, the system SHALL retry with exponential backoff (max 3 retries) and notify the user on final failure
5. WHEN the device is in battery saver mode, the system SHALL defer scheduled crawls until constraints are met

### Requirement 5: Crawl Engine and Network Layer

**User Story:** AS a user, I want reliable and efficient crawling with proper HTTP handling, so that I can crawl various websites successfully.

#### Acceptance Criteria

1. WHEN making HTTP requests, the system SHALL support: GET/POST methods, custom headers, cookies, form data, JSON payloads, and file uploads
2. WHEN encountering redirects, the system SHALL follow up to 10 redirects by default (configurable)
3. WHEN receiving responses, the system SHALL handle: HTML (parse with Jsoup), JSON (parse with Moshi/Gson), XML, and binary content
4. WHILE crawling, the system SHALL respect robots.txt by default (configurable per task)
5. IF a request fails with 4xx/5xx or timeout, the system SHALL retry up to 3 times with configurable delay
6. WHEN crawling multiple URLs, the system SHALL support configurable concurrency (1-10 parallel requests) and per-domain rate limiting
7. IF the response encoding is not UTF-8, the system SHALL auto-detect and convert using ICU4J

### Requirement 6: JavaScript Rendering Support

**User Story:** AS a user, I want to crawl JavaScript-rendered pages, so that I can extract data from modern SPAs and dynamic websites.

#### Acceptance Criteria

1. WHEN a task enables JavaScript rendering, the system SHALL use a headless WebView or Chrome DevTools Protocol to render pages
2. WHEN rendering JavaScript, the system SHALL wait for: network idle, specific selector appearance, or custom JavaScript condition (configurable)
3. IF JavaScript rendering times out (default 30s), the system SHALL fall back to raw HTML or mark as error per task config
4. WHILE using JavaScript rendering, the system SHALL limit concurrent renderers to 2 to manage memory

### Requirement 7: Local Data Storage

**User Story:** AS a user, I want crawl results stored locally on the device, so that I can access data offline and export later.

#### Acceptance Criteria

1. WHEN a crawl extracts data, the system SHALL store results in a Room database with: task ID, URL, extracted fields (JSON), timestamp, and status
2. WHEN viewing results for a task, the system SHALL display paginated, sortable, filterable data with search across fields
3. IF the database exceeds 500MB, the system SHALL prompt the user to archive or delete old results
4. WHEN the user clears results for a task, the system SHALL delete associated records and reclaim storage

### Requirement 8: Data Export

**User Story:** AS a user, I want to export crawl results in multiple formats, so that I can use the data in other tools.

#### Acceptance Criteria

1. WHEN the user chooses to export, the system SHALL offer: CSV (RFC 4180), JSON (array of objects), and Excel (XLSX with proper types)
2. WHEN exporting, the system SHALL allow selecting: all results, filtered results, or specific fields
3. IF the result set exceeds 100,000 rows, the system SHALL stream export to avoid OOM and show progress
4. WHEN export completes, the system SHALL save the file to Downloads folder and offer share intent
5. IF the user grants storage permission, the system SHALL also support exporting to custom directory

### Requirement 9: Data Sync to Server (Optional)

**User Story:** AS a user, I want to optionally sync crawl results to a remote server, so that I can centralize data collection.

#### Acceptance Criteria

1. WHEN the user configures a sync endpoint, the system SHALL allow: REST API URL, authentication (Bearer token, API key, Basic auth), and payload format (JSON)
2. WHEN a crawl completes, the system SHALL optionally trigger sync if enabled for the task
3. IF sync fails, the system SHALL queue for retry (max 5 retries with exponential backoff) and store locally until success
4. WHILE syncing, the system SHALL show sync status in task list (pending/synced/failed)

### Requirement 10: Task Import/Export and Backup

**User Story:** AS a user, I want to backup and restore crawl tasks, so that I can migrate configurations between devices.

#### Acceptance Criteria

1. WHEN the user exports tasks, the system SHALL generate a JSON file containing all task configurations (excluding results)
2. WHEN the user imports a task file, the system SHALL validate the schema and create tasks with new IDs
2. WHEN the user triggers full backup, the system SHALL create an encrypted archive with tasks and optionally results
3. IF import encounters duplicate task names, the system SHALL auto-rename or prompt user

### Requirement 11: Settings and Preferences

**User Story:** AS a user, I want to configure global app behavior, so that the crawler works optimally for my use case.

#### Acceptance Criteria

1. WHEN accessing settings, the system SHALL provide: default User-Agent, request timeout, max redirect count, default concurrency, global rate limit, robots.txt compliance toggle, and JavaScript rendering defaults
2. WHEN the user changes settings, the system SHALL apply to new crawl tasks immediately
3. THE system SHALL provide a "Reset to Defaults" option

### Requirement 12: Security and Privacy

**User Story:** AS a user, I want my crawl data and credentials protected, so that sensitive information is not exposed.

#### Acceptance Criteria

1. WHEN storing authentication credentials (cookies, tokens, passwords), the system SHALL encrypt using Android Keystore (AES-256-GCM)
2. WHEN exporting data containing credentials, the system SHALL prompt for confirmation and offer to exclude sensitive fields
3. THE system SHALL NOT log full request/response bodies containing sensitive data
4. IF the app targets Android 14+, the system SHALL use granular media permissions for exported files

### Requirement 13: Permission Management (Open Source Distribution)

**User Story:** AS a user installing via GitHub Release/F-Droid/APK, I want optional enhanced permissions via ADB grant, so that file export and app detection work seamlessly without repeated system pickers.

#### Acceptance Criteria

1. WHEN the app is installed via sideload, the system SHALL declare `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, `PACKAGE_USAGE_STATS` in Manifest (with `tools:ignore` for Play lint)
2. WHEN a feature requires enhanced permission, the system SHALL check if granted; if not, show a dialog with: "Open Settings" button, "Copy ADB Commands" button, and "Use Standard Mode" fallback button
3. WHEN user chooses "Copy ADB Commands", the system SHALL copy all relevant `adb shell pm grant` commands to clipboard with package name pre-filled
4. WHEN enhanced permission is unavailable, the system SHALL transparently fall back to standard APIs (MediaStore/SAF, `<queries>`, etc.) without functionality loss
5. THE system SHALL provide a "Permission Status" screen showing current grant state for each enhanced permission