plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.crawler"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.crawler"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            shrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = false
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
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
    // Core Android
    val coreKtxVersion = "1.13.0"
    val activityVersion = "1.9.0"
    val fragmentVersion = "1.8.0"
    val lifecycleVersion = "2.8.2"
    val roomVersion = "2.6.1"
    val workVersion = "2.9.0"
    val datastoreVersion = "1.1.2"

    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.activity:activity-compose:$activityVersion")
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Room Database
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:$workVersion")

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
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.paging:paging-compose:3.3.0")

    // Hilt
    val hiltVersion = "2.48"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Network
    val okhttpVersion = "4.12.0"
    val retrofitVersion = "2.11.0"
    val moshiVersion = "1.15.1"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // XPath
    implementation("com.github.wnameless:xpath:1.1.0")

    // ICU4J for charset detection
    implementation("com.ibm.icu:icu4j:75.1")

    // Cron4j for scheduling
    implementation("it.sauronsoftware:cron4j:2.2.5")

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

    // Security Crypto
    val securityVersion = "1.1.0-alpha06"
    implementation("androidx.security:security-crypto:$securityVersion")
    implementation("androidx.security:security-identity-credential:$securityVersion")

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
    kaptAndroidTest("com.google.dagger:hilt-compiler:$hiltVersion")
}

kapt {
    correctErrorTypes = true
    javacOptions {
        option("-Xlint:unchecked")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.datetime.ExperimentalDateTimeApi",
            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

tasks.named("compileDebugKotlin") {
    kotlinOptions.freeCompilerArgs += "-Pplugin:kotlinx-serialization=explicit"
}