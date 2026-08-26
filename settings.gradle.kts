pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:8.5.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:1.9.24")
            }
            if (requested.id.id == "org.jetbrains.kotlin.kapt") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "CrawlerApp"
include(":app")