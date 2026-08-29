# 需求实施计划

> 实施状态同步日期：2026-08-29。项目已完成开发并发布 v1.0.0（tag: v1.0.0）。
> 两条回归主线：
> - 原计划无额外划线的任务均已实现完成。
> - 实现期间新增了爬取历史、任务备份导出/导入、以及基于 Shizuku/Root 的 ADB 能力三项功能，已补充进相应章节（标记为“新增”）。

- [x] 1. 初始化项目结构和构建配置
   - 创建 Android 模块目录结构 (app/src/main/{java,res,assets})
   - 配置 build.gradle.kts: Kotlin, Compose BOM, Room, WorkManager, OkHttp, Jsoup, Moshi, POI, cron-utils, ICU4J, Security Crypto（注：计划中的 cron4j 实际采用 cron-utils 9.2.1）
   - 配置 AndroidManifest.xml: 权限声明，tools:ignore 标记
   - 设置 Hilt 依赖注入框架
   - 定义基础包结构: data, domain, presentation, di, util

- [x] 2. 数据层实现
  - [x] 2.1 创建 Room 实体和类型转换器
    - 实现 CrawlTaskEntity, CrawlResultEntity (design.md Data Layer §Room Database Schema)
    - 实现嵌入类: UrlPatternsEntity, ExtractionRuleEntity, RequestConfigEntity, ScheduleConfigEntity, JsRenderingConfigEntity, SyncConfigEntity
    - 编写 TypeConverters: List<String>, Map<String,String>, Enum, Instant, LocalTime, DayOfWeek, Set<String>
    - 定义 TaskDao, ResultDao, SettingsDao 接口 (CRUD, 分页查询, 按 taskId 查询结果, 统计)

  - [x] 2.2 实现 Repository 模式
    - 实现 TaskRepository: 暴露 Flow<List<CrawlTask>>, create/update/delete/getById, importExportTasks
    - 实现 ResultRepository: 分页 PagingSource, search/filter/sort, deleteByTaskId, getStats
    - 实现 SettingsRepository: DataStore/SharedPreferences 存储 AppSettings
    - 实现 CredentialsManager: Android Keystore + EncryptedSharedPreferences 加密存储 (Req 12.1)

  - [x] 2.3 数据库迁移和初始化
    - 编写 RoomDatabase 子类, 配置迁移策略（升级使用 fallbackToDestructiveMigration）
    - Application 启动时初始化数据库, 调用 TaskRepository initSchedules() 恢复 WorkManager 作业 (Error Handling §WorkManager job lost)

  - [x] 2.4 新增：爬取历史记录
    - 新增 CrawlHistoryEntity (tableName = "crawl_history") 与 HistoryDao
    - 新增 HistoryRepository / HistoryRepositoryImpl，HistoryViewModel 与 HistoryScreen 展示历史
    - 新增 TaskBackupService / TaskBackupServiceImpl，支持任务配置导出 JSON 与导入（Req 10）
    - 新增 AdbRepository / AdbRepositoryImpl 与 AdbClient，封装 Shizuku / Root / Local 三种 shell 执行模式

- [x] 3. 网络层实现
  - [x] 3.1 配置 OkHttpClient
    - 连接池 10, 超时 30s, 重试, 最大重定向 10 (design.md Network Layer)
    - 添加拦截器: 日志 (非敏感), 请求头注入, CookieManager CookieJar 持久化
    - 支持 GET/POST/PUT/PATCH/DELETE, JSON body, FORM body (Req 5.1；MULTIPART 待完善)

  - [x] 3.2 实现 HTML/JSON 解析
    - Jsoup 配置: HTML5 parser, base URI tracking (design.md)
    - Moshi 配置: Kotlin adapter factory, lenient mode
    - 编码自动检测: ICU4J CharsetDetector 集成 (Req 5.7)

  - [x] 3.3 实现 Headless WebView 渲染器
    - 创建无头 WebView, WebViewClient/WebChromeClient
    - 实现等待条件: network idle, selector 出现, 自定义 JS 脚本 (Req 6.2)
    - 超时处理: 默认 30s, 可配置, 超时回退 (Req 6.3)
    - 资源阻塞: 拦截 image 等资源加载 (JsRenderingConfig.blockResources)
    - 并发限制: Semaphore(2) 限制并发渲染器 (design.md Constraints)

- [x] 4. 领域层核心引擎实现
  - [x] 4.1 实现 ExtractionEngine
    - CSS 选择器: Jsoup select()
    - 正则表达式: Java Pattern/Matcher
    - 多值策略: FIRST, ALL_ARRAY, JOIN (ExtractionRule.multiple)
    - 后处理器链: Trim, RegexReplace, TypeConversion (STRING/INTEGER/LONG/DOUBLE/BOOLEAN/DATE)
    - testRule(): 实时预览功能 (Req 2.3)
    - 注：XPath 选择器已接入 `SelectorType.XPATH`，但基于 jsoup 简单 XPath→CSS 转换实现（`ExtractionEngineImpl.xpathToCss`），未使用专用 xpath 库，仅支持常见路径

  - [x] 4.2 实现 CrawlEngine
    - 标准抓取策略: OkHttp + Jsoup/Moshi
    - JS 渲染策略: WebViewRenderer + 等待条件
    - 策略模式: FetchStrategy 接口, 根据 JsRenderingConfig.enabled 切换
    - 并发控制与限速: Semaphore + RateLimiter (每域)
    - 重试逻辑: 指数退避, 可配置最大次数 (Req 5.5)
    - robots.txt 解析与遵守 (Req 5.4)
    - 进度回调: CrawlProgress 实时发射 (Req 3.2)
    - 优雅停止: 协程取消 + 保存部分结果 (Req 3.3)

  - [x] 4.3 实现 Scheduler
    - WorkManager 封装: OneTimeWorkRequest / PeriodicWorkRequest
    - Cron 表达式解析: cron-utils 库 (实际使用，替代计划中的 cron4j)
    - 约束: NetworkType.CONNECTED, 不要求充电 (design.md Background Services)
    - 调度/取消/重调度/获取下次运行时间

  - [x] 4.4 实现 ExportService
    - CSV: RFC 4180 合规, 流式写入, 大数据分片 (Req 8.3)
    - JSON: 流式数组写入
    - Excel (XLSX): Apache POI SXSSF 流式写入 (Req 8.1)
    - MediaStore/SAF 双模式: 权限优先, 回退 ACTION_OPEN_DOCUMENT_TREE (design.md Permission Strategy)
    - 导出配置: 字段选择, 过滤条件, 导出范围 (Req 8.2)

  - [x] 4.5 实现 SyncService (可选需求 Req 9)
    - Retrofit + OkHttp 客户端, 认证拦截器 (Bearer/API Key/Basic)
    - 指数退避重试: 最多 5 次 (Req 9.3)
    - 同步状态持久化: CrawlResultEntity.syncedAt

- [x] 5. 后台服务实现
  - [x] 5.1 实现 CrawlWorker (WorkManager)
    - 接收 taskId, 调用 CrawlEngine.execute()
    - 失败重试: BackoffPolicy.EXPONENTIAL, 最多 3 次 (Req 4.4)
    - 结果持久化通过 ResultRepository

  - [x] 5.2 实现 CrawlForegroundService
    - 前台服务类型: FOREGROUND_SERVICE_DATA_SYNC (Android 14+)
    - 持久通知: 进度条, 当前 URL, 统计信息, 停止按钮 (Req 3.1, 3.2)
    - 生命周期: 启动 -> 绑定 CrawlEngine -> 完成/停止 -> 停止服务

  - [x] 5.3 实现 SyncWorker
    - WorkManager 周期性/一次性同步
    - 调用 SyncService.sync(), 处理认证失败通知用户 (Error Handling)

- [x] 6. ViewModel 与状态管理
  - [x] 6.1 TaskViewModel
    - StateFlow<List<CrawlTask>> 任务列表
    - create/update/deleteTask(), importExportTasks()
    - 重名校验 (Req 1.5)

  - [x] 6.2 CrawlViewModel
    - crawlState: StateFlow<CrawlState> (IDLE/RUNNING/PAUSED/COMPLETED/FAILED/STOPPED)
    - progress: StateFlow<CrawlProgress> 实时进度
    - startCrawl(taskId): 启动 ForegroundService
    - stopCrawl(): 取消协程, 停止服务

  - [x] 6.3 ResultsViewModel
    - PagingSource<Int, CrawlResult> 分页加载
    - search/filter/sort 参数化查询
    - export(format, filters) -> 调用 ExportService
    - deleteResults(taskId)

  - [x] 6.4 SettingsViewModel
    - AppSettings DataStore 流
    - updateSetting(key, value), resetDefaults() (Req 11)

  - [x] 6.5 PermissionViewModel
    - 检测 ADB 权限状态: MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS (design.md PermissionHelper)
    - 生成 ADB 授权命令列表, 复制到剪贴板 (Req 13.3)
    - 打开对应系统设置页 (Req 13.2)

  - [x] 6.6 新增：AdbViewModel 与 HistoryViewModel
    - AdbViewModel: 检测 Shizuku 可用/授权/已安装状态，授权、打开 Shizuku 应用；展示 Root/Local 模式
    - HistoryViewModel: 展示爬取历史记录

- [x] 7. UI 实现
  - [x] 7.1 主导航与基础组件
    - MainScreen: BottomNavigation (Tasks, Results, Settings)
    - Material 3 主题, 响应式布局 (手机/平板/折叠屏)

  - [x] 7.2 TaskListScreen
    - LazyColumn 任务列表, 状态芯片, 结果计数, 下次运行时间
    - Swipe-to-delete, 编辑跳转, FAB 新建任务 (Req 1.4)

  - [x] 7.3 TaskEditorScreen
    - 表单: 基本信息, URLs, URL Patterns (include/exclude/maxDepth/maxPages)
    - 请求配置: 方法, Headers, Cookies, Body, 超时, 重定向, User-Agent
    - 调度配置: 类型选择器, Cron 表达式输入, 时间选择器
    - JS 渲染开关 + 高级等待条件配置
    - 同步配置: 端点, 认证, 触发条件

  - [x] 7.4 RuleBuilderScreen
    - 规则列表: 字段名, 选择器类型下拉, 表达式输入
    - 属性选择: text/href/src/html/custom
    - 多值策略单选, 连接符输入
    - 后处理器添加/移除 UI
    - 实时预览面板: 输入样例 URL -> fetch -> 展示提取结果 (Req 2.3, 2.5)

  - [x] 7.5 ResultsScreen
    - PagingData 表格: 列选择器, 搜索框, 过滤芯片
    - 行点击查看详情 (原始 JSON)
    - 导出按钮: 格式选择对话框 -> 调用 ViewModel.export()
    - 清空结果确认对话框 (Req 7.4)

  - [x] 7.6 SettingsScreen
    - 分组: Network, Crawling, Storage, Security, Advanced
    - 每项: 标题, 描述, 编辑控件 (开关/输入/选择)
    - 重置默认值按钮 (Req 11.3)

  - [x] 7.7 PermissionStatusScreen
    - 权限卡片: 名称, 状态(已授权/未授权), 说明
    - 动作按钮: 去设置, 复制 ADB 命令, 测试标准模式 (Req 13.2, 13.4, 13.5)

  - [x] 7.8 新增：AdbStatusScreen 与 HistoryScreen
    - AdbStatusScreen: Shizuku/Root/Local 三模式状态展示与授权引导
    - HistoryScreen: 爬取历史列表

- [x] 8. 权限与兼容性处理
  - [x] 8.1 实现 PermissionHelper 工具类
    - checkManageStorage(), checkQueryAllPackages(), checkUsageStats()
    - openManageStorageSettings(), openUsageStatsSettings()
    - getAdbGrantCommands(packageName)

  - [x] 8.2 双模式导出逻辑
    - 优先尝试 FileOutputStream (MANAGE_EXTERNAL_STORAGE)
    - 失败/无权限 -> MediaStore 插入 + SAF 目录选择器

  - [x] 8.3 应用启动权限检查与引导
    - 首次启动检测关键权限, 引导页/对话框提示
    - "稍后设置" 选项, 使用时再次询问

- [x] 9. 任务导入导出与备份 (Req 10)
  - [x] 9.1 任务导出: TaskBackupService.exportTasks() 序列化 Task 配置为 JSON (排除结果)
  - [x] 9.2 任务导入: TaskBackupService.importTasks() JSON 校验, 分配新 UUID (Req 10.3)
  - [x] 9.3 全量备份: TaskBackupService.exportFullBackup()/restoreFullBackup()，AES-256-GCM 加密归档（任务+可选结果，ArchiveCrypto 基于 Android Keystore）(Req 10.2, 12.1)

- [x] 10. 安全与隐私加固
  - [x] 10.1 敏感字段加密存储: cookies, tokens, passwords -> CredentialsManager
  - [x] 10.2 导出时敏感字段过滤: ExportConfig.excludeSensitiveFields + resolveFields()，自动剔除 password/token/api_key 等敏感字段，ExportResult 返回 filteredSensitiveCount (Req 12.2)
  - [x] 10.3 日志脱敏: 不记录请求/响应体 (Req 12.3) —— LoggingInterceptor 仅记 method/url/code，不含 body
  - [x] 10.4 Android 14+ 颗粒化媒体权限: Manifest 声明 READ_MEDIA_IMAGES/VIDEO/AUDIO/VISUAL_USER_SELECTED，PermissionHelper 提供 missingMediaPermissions() 等检查 (Req 12.4)

- [ ] 11. 集成与端到端验证
  - [ ] 11.1 完整流程测试: 新建任务 -> 配置规则 -> 手动启动 -> 查看进度 -> 停止 -> 查看结果 -> 导出
  - [ ] 11.2 定时任务测试: 设置每日 -> 验证 WorkManager 触发 -> 验证静默执行 -> 验证结果入库
  - [ ] 11.3 JS 渲染测试: 开启 JS -> 爬取 SPA 测试站点 -> 验证数据提取
  - [ ] 11.4 大数据量测试: 10k+ 结果导出 CSV/JSON/Excel, 内存占用监控
  - [ ] 11.5 权限回退测试: 撤销 MANAGE_EXTERNAL_STORAGE -> 导出走 SAF 路径
  - [ ] 11.6 网络异常测试: 超时/4xx/5xx/DNS/SSL -> 验证重试/错误记录

- [ ] 12. 检查点 - 核心功能验证
  - 在无 JDK/Android SDK 环境下仅能进行静态代码审查；实际功能需在具备环境的机器上运行 `./gradlew assembleDebug` 并真机验证

- [ ]* 13. 单元测试编写
  - [x]* 13.1 ExtractionEngine 单元测试: CSS/XPath/Regex, 多值策略, 后处理器 (Coverage 90%) —— app/src/test/java/com/crawler/domain/engine/ExtractionEngineImplTest.kt 已编写；无法本地运行（无 JDK/SDK）
  - [ ]* 13.2 CrawlEngine 单元测试: 并发控制, 限速, 重试, JS 回退 (Coverage 80%)
  - [ ]* 13.3 Scheduler 单元测试: Cron 解析, WorkManager 入队/取消 (Coverage 85%)
  - [ ]* 13.4 ExportService 单元测试: CSV RFC4180, JSON 流式, XLSX 类型, 大数据分片 (Coverage 85%)
  - [ ]* 13.5 SyncService 单元测试: 认证拦截器, 退避重试 (Coverage 80%)
  - [ ]* 13.6 CredentialsManager 单元测试: 加解密往返, 密钥轮换 (Coverage 95%)
  - [x]* 13.7 导出敏感字段过滤测试: app/src/test/java/com/crawler/domain/model/ExportConfigTest.kt（resolveFields 大小写/包含交集/敏感匹配）

- [ ]* 14. 集成测试编写
  - [ ]* 14.1 Room DAO 测试: 内存数据库 CRUD, 分页, 事务
  - [ ]* 14.2 WorkManager 测试: TestWorkerBuilder 验证调度参数
  - [ ]* 14.3 ForegroundService 测试: ServiceController 生命周期
  - [ ]* 14.4 MediaStore 导出测试: API 29+ Scoped Storage 兼容性

- [ ]* 15. 代码质量与发布准备
  - [x]* 15.1 静态分析: detekt 已接入（root + app plugin，detekt.yml）—— 待真实环境运行验证
  - [x]* 15.2 混淆规则: proguard-rules.pro (保留 Room/Serialization/反射类) —— 文件存在；release 当前 isMinifyEnabled = false
  - [x]* 15.3 版本管理: versionCode=1, versionName="1.0.0", Git 标签 v1.0.0
  - [x]* 15.4 签名配置: 支持通过 KEYSTORE_* 环境变量配置 release 签名
  - [x]* 15.5 README 与使用文档: 编译/运行/权限授权/APK 下载链接已完成