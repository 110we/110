plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.crawler"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        val keystorePath = System.getenv("KEYSTORE_PATH")
        if (keystorePath != null && keystorePath.isNotBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null && keystorePath.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packagingOptions {
        resources {
            excludes += listOf("META-INF/*")
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    namespace = "com.crawler"
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android
    val coreKtxVersion = "1.13.0"
    val activityVersion = "1.9.0"
    val fragmentVersion = "1.8.0"
    val lifecycleVersion = "2.8.2"
    val roomVersion = "2.6.1"
    val workVersion = "2.9.1"
    val datastoreVersion = "1.1.4"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")
    implementation("androidx.datastore:datastore-core:$datastoreVersion")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.paging:paging-compose:3.3.0")

    // Hilt
    val hiltVersion = "2.48"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Network
    val okhttpVersion = "4.12.0"
    val retrofitVersion = "2.11.0"
    val moshiVersion = "1.15.1"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.24")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // ICU4J for charset detection
    implementation("com.ibm.icu:icu4j:75.1")

    // Cron - using cron-utils from Maven Central
    implementation("com.cronutils:cron-utils:9.2.1")

    // Coroutines
    val coroutinesVersion = "1.8.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Apache POI for Excel export
    val poiVersion = "5.3.0"
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    // Shizuku for ADB access
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Security Crypto
    val securityVersion = "1.1.0-alpha06"
    implementation("androidx.security:security-crypto:$securityVersion")

    // Coil for image loading (if needed)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    val junitVersion = "4.13.2"
    val junitJupiterVersion = "5.10.2"
    val mockkVersion = "1.13.13"
    val truthVersion = "1.1.5"
    val espressoVersion = "3.5.1"
    val composeTestVersion = "2024.06.00"

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.work:work-testing:$workVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeTestVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:$composeTestVersion")
    androidTestImplementation("androidx.activity:activity-compose:$activityVersion")
    androidTestImplementation("androidx.lifecycle:lifecycle-runtime-testing:$lifecycleVersion")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kspAndroidTest("com.google.dagger:hilt-compiler:$hiltVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}