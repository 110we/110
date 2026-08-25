# 需求实施计划

- [ ] 1. 初始化项目结构和构建配置
   - 创建 Android 模块目录结构 (app/src/main/{java,res,assets})
   - 配置 build.gradle.kts: Kotlin 2.0+, Compose BOM, Room, WorkManager, OkHttp, Jsoup, Moshi, POI, cron4j, ICU4J, Security Crypto
   - 配置 AndroidManifest.xml: 权限声明 (MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS, REQUEST_INSTALL_PACKAGES, FOREGROUND_SERVICE_DATA_SYNC, INTERNET, ACCESS_NETWORK_STATE), tools:ignore 标记
   - 设置 Hilt/Koin 依赖注入框架
   - 定义基础包结构: data, domain, presentation, di, util

- [ ] 2. 数据层实现
  - [ ] 2.1 创建 Room 实体和类型转换器
    - 实现 CrawlTaskEntity, CrawlResultEntity (design.md Data Layer §Room Database Schema)
    - 实现嵌入类: UrlPatternsEntity, ExtractionRuleEntity, RequestConfigEntity, ScheduleConfigEntity, JsRenderingConfigEntity, SyncConfigEntity
    - 编写 TypeConverters: List<String>, Map<String,String>, Enum, Instant, LocalTime, DayOfWeek, Set<String>
    - 定义 TaskDao, ResultDao, SettingsDao 接口 (CRUD, 分页查询, 按 taskId 查询结果, 统计)

  - [ ] 2.2 实现 Repository 模式
    - 实现 TaskRepository: 暴露 Flow<List<CrawlTask>>, create/update/delete/getById, importExportTasks
    - 实现 ResultRepository: 分页 PagingSource, search/filter/sort, deleteByTaskId, getStats
    - 实现 SettingsRepository: DataStore/SharedPreferences 存储 AppSettings
    - 实现 CredentialsManager: Android Keystore + EncryptedSharedPreferences 加密存储 (Req 12.1)

  - [ ] 2.3 数据库迁移和初始化
    - 编写 RoomDatabase 子类, 配置迁移策略
    - Application 启动时初始化数据库, 调用 TaskRepository.initSchedules() 恢复 WorkManager 作业 (Error Handling §WorkManager job lost)

- [ ] 3. 网络层实现
  - [ ] 3.1 配置 OkHttpClient
    - 连接池 10, 超时 30s, 重试 3 次, 最大重定向 10 (design.md Network Layer)
    - 添加拦截器: 日志 (非敏感), 请求头注入, CookieJar 持久化
    - 支持 GET/POST/PUT/PATCH/DELETE, multipart/form-data, JSON body (Req 5.1)

  - [ ] 3.2 实现 HTML/JSON 解析
    - Jsoup 配置: HTML5 parser, base URI tracking (design.md)
    - Moshi 配置: Kotlin adapter factory, lenient mode
    - 编码自动检测: ICU4J CharsetDetector 集成 (Req 5.7)

  - [ ] 3.3 实现 Headless WebView 渲染器
    - 创建无头 WebView (offscreen mode), WebViewClient/WebChromeClient
    - 实现等待条件: network idle, selector 出现, 自定义 JS 脚本 (Req 6.2)
    - 超时处理: 默认 30s, 可配置, 超时回退原始 HTML 或报错 (Req 6.3)
    - 资源阻塞: 拦截 image/stylesheet/font/media 请求 (JsRenderingConfig.blockResources)
    - 并发限制: 固定线程池 2 个渲染器 (design.md Constraints)

- [ ] 4. 领域层核心引擎实现
  - [ ] 4.1 实现 ExtractionEngine
    - CSS 选择器: Jsoup select()
    - XPath 选择器: 集成 xpath 库 (如 com.github.wnameless:xpath)
    - 正则表达式: Java Pattern/Matcher
    - 多值策略: FIRST, ALL_ARRAY, JOIN (ExtractionRule.multiple)
    - 后处理器链: Trim, RegexReplace, TypeConversion (STRING/INTEGER/LONG/DOUBLE/BOOLEAN/DATE)
    - testRule(): 实时预览功能 (Req 2.3)

  - [ ] 4.2 实现 CrawlEngine
    - 标准抓取策略: OkHttp + Jsoup/Moshi
    - JS 渲染策略: WebViewRenderer + 等待条件
    - 策略模式: FetchStrategy 接口, 根据 JsRenderingConfig.enabled 切换
    - 并发控制: Semaphore(1-10) + 每域限速 TokenBucket
    - 重试逻辑: 指数退避 2s/4s/8s, 可配置最大次数 (Req 5.5)
    - robots.txt 解析与遵守 (Req 5.4)
    - 进度回调: CrawlProgress 实时发射 (Req 3.2)
    - 优雅停止: 协程取消 + 保存部分结果 (Req 3.3)

  - [ ] 4.3 实现 Scheduler
    - WorkManager 封装: OneTimeWorkRequest / PeriodicWorkRequest
    - Cron 表达式解析: cron4j 库 (Req 4.1 CUSTOM 类型)
    - 约束: NetworkType.CONNECTED, 不要求充电 (design.md Background Services)
    - 调度/取消/重调度/获取下次运行时间

  - [ ] 4.4 实现 ExportService
    - CSV: RFC 4180 合规, 流式写入, 大数据分片 100k 行 (Req 8.3)
    - JSON: 流式数组写入
    - Excel (XLSX): Apache POI SXSSF 流式写入, 类型映射
    - MediaStore/SAF 双模式: MANAGE_EXTERNAL_STORAGE 优先, 回退 ACTION_OPEN_DOCUMENT_TREE (design.md Permission Strategy)
    - 导出配置: 字段选择, 过滤条件, 导出范围 (Req 8.2)

  - [ ] 4.5 实现 SyncService (可选需求 Req 9)
    - Retrofit + OkHttp 客户端, 认证拦截器 (Bearer/API Key/Basic)
    - 指数退避重试: 最多 5 次 (Req 9.3)
    - 同步状态持久化: CrawlResultEntity.syncedAt

- [ ] 5. 后台服务实现
  - [ ] 5.1 实现 CrawlWorker (WorkManager)
    - 接收 taskId, 调用 CrawlEngine.execute()
    - 静默执行: 无前台通知 (Req 4.3)
    - 失败重试: BackoffPolicy.EXPONENTIAL, 最多 3 次 (Req 4.4)
    - 结果持久化通过 ResultRepository

  - [ ] 5.2 实现 CrawlForegroundService
    - 前台服务类型: FOREGROUND_SERVICE_DATA_SYNC (Android 14+)
    - 持久通知: 进度条, 当前 URL, 统计信息, 停止按钮 (Req 3.1, 3.2)
    - 生命周期: 启动->绑定 CrawlEngine->完成/停止->停止服务
    - 后台存活: startForeground, PARTIAL_WAKE_LOCK (可选)

  - [ ] 5.3 实现 SyncWorker
    - WorkManager 周期性/一次性同步
    - 调用 SyncService.sync(), 处理认证失败通知用户 (Error Handling)

- [ ] 6. ViewModel 与状态管理
  - [ ] 6.1 TaskViewModel
    - StateFlow<List<CrawlTask>> 任务列表
    - create/update/deleteTask(), importExportTasks()
    - 重名校验 (Req 1.5)

  - [ ] 6.2 CrawlViewModel
    - crawlState: StateFlow<CrawlState> (IDLE/RUNNING/PAUSED/COMPLETED/FAILED/STOPPED)
    - progress: StateFlow<CrawlProgress> 实时进度
    - startCrawl(taskId): 启动 ForegroundService
    - stopCrawl(): 取消协程, 停止服务

  - [ ] 6.3 ResultsViewModel
    - PagingSource<Int, CrawlResult> 分页加载
    - search/filter/sort 参数化查询
    - export(format, filters) -> 调用 ExportService
    - deleteResults(taskId)

  - [ ] 6.4 SettingsViewModel
    - AppSettings DataStore 流
    - updateSetting(key, value), resetDefaults() (Req 11)

  - [ ] 6.5 PermissionViewModel
    - 检测 ADB 权限状态: MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS (design.md PermissionHelper)
    - 生成 ADB 授权命令列表, 复制到剪贴板 (Req 13.3)
    - 打开对应系统设置页 (Req 13.2)

- [ ] 7. UI 实现
  - [ ] 7.1 主导航与基础组件
    - MainScreen: BottomNavigation (Tasks, Results, Settings)
    - Material 3 主题, 响应式布局 (手机/平板/折叠屏)

  - [ ] 7.2 TaskListScreen
    - LazyColumn 任务列表, 状态芯片, 结果计数, 下次运行时间
    - Swipe-to-delete, 编辑跳转, FAB 新建任务 (Req 1.4)

  - [ ] 7.3 TaskEditorScreen
    - 表单: 基本信息, URLs, URL Patterns (include/exclude/maxDepth/maxPages)
    - 请求配置: 方法, Headers, Cookies, Body, 超时, 重定向, User-Agent
    - 调度配置: 类型选择器, Cron 表达式输入, 时间选择器
    - JS 渲染开关 + 高级等待条件配置
    - 同步配置: 端点, 认证, 触发条件

  - [ ] 7.4 RuleBuilderScreen
    - 规则列表: 字段名, 选择器类型下拉, 表达式输入
    - 属性选择: text/href/src/html/custom
    - 多值策略单选, 连接符输入
    - 后处理器添加/移除 UI
    - 实时预览面板: 输入样例 URL -> fetch -> 展示提取结果 (Req 2.3, 2.5)

  - [ ] 7.5 ResultsScreen
    - PagingData 表格: 列选择器, 搜索框, 过滤芯片
    - 行点击查看详情 (原始 JSON)
    - 导出按钮: 格式选择对话框 -> 调用 ViewModel.export()
    - 清空结果确认对话框 (Req 7.4)

  - [ ] 7.6 SettingsScreen
    - 分组: Network, Crawling, Storage, Security, Advanced
    - 每项: 标题, 描述, 编辑控件 (开关/输入/选择)
    - 重置默认值按钮 (Req 11.3)

  - [ ] 7.7 PermissionStatusScreen
    - 权限卡片: 名称, 状态(已授权/未授权), 说明
    - 动作按钮: 去设置, 复制 ADB 命令, 测试标准模式 (Req 13.2, 13.4, 13.5)

- [ ] 8. 权限与兼容性处理
  - [ ] 8.1 实现 PermissionHelper 工具类
    - checkManageStorage(), checkQueryAllPackages(), checkUsageStats()
    - openManageStorageSettings(), openUsageStatsSettings()
    - getAdbGrantCommands(packageName)

  - [ ] 8.2 双模式导出逻辑
    - 优先尝试 FileOutputStream (MANAGE_EXTERNAL_STORAGE)
    - 失败/无权限 -> MediaStore 插入 + SAF 目录选择器

  - [ ] 8.3 应用启动权限检查与引导
    - 首次启动检测关键权限, 引导页/对话框提示
    - "稍后设置" 选项, 使用时再次询问

- [ ] 9. 任务导入导出与备份
  - [ ] 9.1 任务导出: 序列化 Task 配置为 JSON (排除结果)
  - [ ] 9.2 任务导入: JSON 校验, 重名自动重命名 (Req 10.3), 分配新 UUID
  - [ ] 9.3 全量备份: 加密归档 (任务+可选结果), AES-256-GCM (Req 10.2, 12.1)

- [ ] 10. 安全与隐私加固
  - [ ] 10.1 敏感字段加密存储: cookies, tokens, passwords -> CredentialsManager
  - [ ] 10.2 导出时敏感字段过滤提示 (Req 12.2)
  - [ ] 10.3 日志脱敏: 请求/响应体不记录敏感字段 (Req 12.3)
  - [ ] 10.4 Android 14+ 颗粒化媒体权限: READ_MEDIA_IMAGES/VIDEO/DOCUMENTS (Req 12.4)

- [ ] 11. 集成与端到端验证
  - [ ] 11.1 完整流程测试: 新建任务 -> 配置规则 -> 手动启动 -> 查看进度 -> 停止 -> 查看结果 -> 导出
  - [ ] 11.2 定时任务测试: 设置每日 -> 验证 WorkManager 触发 -> 验证静默执行 -> 验证结果入库
  - [ ] 11.3 JS 渲染测试: 开启 JS -> 爬取 SPA 测试站点 -> 验证数据提取
  - [ ] 11.4 大数据量测试: 10k+ 结果导出 CSV/JSON/Excel, 内存占用监控
  - [ ] 11.5 权限回退测试: 撤销 MANAGE_EXTERNAL_STORAGE -> 导出走 SAF 路径
  - [ ] 11.6 网络异常测试: 超时/4xx/5xx/DNS/SSL -> 验证重试/错误记录

- [ ] 12. 检查点 - 核心功能验证
  - 确保所有核心功能可运行, 如有疑问请询问用户

- [ ]* 13. 单元测试编写
  - [ ]* 13.1 ExtractionEngine 单元测试: CSS/XPath/Regex, 多值策略, 后处理器 (Coverage 90%)
  - [ ]* 13.2 CrawlEngine 单元测试: 并发控制, 限速, 重试, JS 回退 (Coverage 80%)
  - [ ]* 13.3 Scheduler 单元测试: Cron 解析, WorkManager 入队/取消 (Coverage 85%)
  - [ ]* 13.4 ExportService 单元测试: CSV RFC4180, JSON 流式, XLSX 类型, 大数据分片 (Coverage 85%)
  - [ ]* 13.5 SyncService 单元测试: 认证拦截器, 退避重试 (Coverage 80%)
  - [ ]* 13.6 CredentialsManager 单元测试: 加解密往返, 密钥轮换 (Coverage 95%)

- [ ]* 14. 集成测试编写
  - [ ]* 14.1 Room DAO 测试: 内存数据库 CRUD, 分页, 事务
  - [ ]* 14.2 WorkManager 测试: TestWorkerBuilder 验证调度参数
  - [ ]* 14.3 ForegroundService 测试: ServiceController 生命周期
  - [ ]* 14.4 MediaStore 导出测试: API 29+ Scoped Storage 兼容性

- [ ]* 15. 代码质量与发布准备
  - [ ]* 15.1 静态分析: detekt, lint, ktlint
  - [ ]* 15.2 混淆规则: proguard-rules.pro (保留 Room/Serialization/反射类)
  - [ ]* 15.3 版本管理: versionCode/versionName, Git 标签
  - [ ]* 15.4 签名配置: debug/release keystore
  - [ ]* 15.5 README 与使用文档: 编译/运行/权限授权/常见问题