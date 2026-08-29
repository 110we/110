# CrawlerApp

Android 爬虫应用，支持任务调度、数据采集、结果导出（Excel）等功能。

## 下载 APK

直接下载（由 GitHub Actions 编译、Release 发布）：**app-debug.apk**

[https://github.com/110we/110/releases/download/v1.0.0/app-debug.apk](https://github.com/110we/110/releases/download/v1.0.0/app-debug.apk)

## 目录结构

```
.
├── app/                        # Android 应用模块
│   ├── build.gradle.kts        # 应用构建配置
│   ├── proguard-rules.pro      # R8/ProGuard 混淆规则
│   └── src/main/               # 源码（Kotlin + Jetpack Compose）
├── build.gradle.kts            # 根构建配置
├── settings.gradle.kts         # Gradle 设置
├── gradlew                     # Gradle Wrapper
└── .github/workflows/          # GitHub Actions 工作流
```

## 构建 APK

### 通过 GitHub Actions（推荐）

推送到 `main` 或 `master` 分支、创建 Pull Request、或手动触发 `Build APK` 工作流，即可在 GitHub 上自动构建 APK：

1. 将代码推送到 GitHub 仓库
2. 进入仓库的 **Actions** 页面，找到 **Build APK** 工作流
3. 点击 **Run workflow** 手动触发，或直接推送代码自动触发
4. 构建完成后，在本次运行的 **Artifacts** 中下载：
   - `app-debug` — Debug 版 APK（可直接安装）
   - `app-release` — Release 版 APK（未签名）

如需签名 Release 版，在仓库 Settings → Secrets and variables → Actions 中配置以下密钥，并设置 `KEYSTORE_PATH` 环境变量指向签名文件（工作流已预留 `KEYSTORE_PATH`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 传参）。

### 本地构建

```bash
# 安装 JDK 17 和 Android SDK（compileSdk 34）
chmod +x gradlew
./gradlew assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`，可直接安装。

## 技术栈

- Kotlin 2.0 + Jetpack Compose
- Hilt 依赖注入 + Room 数据库
- Retrofit / OkHttp / Moshi 网络层
- WorkManager 后台任务
- jsoup HTML 解析 + Apache POI Excel 导出
