# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Moshi
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keepnames @com.squareup.moshi.JsonClass class *
-keep class **JsonAdapter { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.crawler.**$$serializer { *; }
-keepclassmembers class com.crawler.** {
    *** Companion;
}
-keepclasseswithmembers class com.crawler.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * implements androidx.room.Dao

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.BaseApplication

# Apache POI
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.** { *; }
-keep class org.openxmlformats.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**
-dontwarn org.openxmlformats.**

# Optional/transitive dependencies referenced from Apache POI and log4j-api
-dontwarn org.apache.logging.log4j.**
-dontwarn aQute.bnd.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn org.osgi.framework.**
-dontwarn java.awt.**

# Tink (androidx.security-crypto) optional annotations
-dontwarn com.google.errorprone.**

# ICU4J
-keep class com.ibm.icu.** { *; }
-dontwarn com.ibm.icu.**

# cron-utils
-keep class com.cronutils.** { *; }
-dontwarn com.cronutils.**

# jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.reflect.** { *; }

# Application
-keep class com.crawler.** { *; }

# Workaround for R8 ConcurrentModificationException on POI's java.awt references
# (getViewbox() returns java.awt.geom.Rectangle2D which is absent on Android)
-dontoptimize
-ignorewarnings
