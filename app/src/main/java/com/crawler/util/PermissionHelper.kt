package com.crawler.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import timber.log.Timber

class PermissionHelper(private val context: Context) {

    fun checkManageStorage(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun checkQueryAllPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_QUERY_ALL_PACKAGES)
        } else {
            true
        }
    }

    fun checkUsageStats(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName)
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        }
        return false
    }

    fun checkInstallPackages(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.REQUEST_INSTALL_PACKAGES) == PackageManager.PERMISSION_GRANTED
    }

    fun openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .apply { data = Uri.parse("package:${context.packageName}") }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInstallPackagesSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .apply { data = Uri.parse("package:${context.packageName}") }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun getAdbGrantCommands(packageName: String): List<String> {
        return listOf(
            "adb shell pm grant $packageName android.permission.MANAGE_EXTERNAL_STORAGE",
            "adb shell pm grant $packageName android.permission.QUERY_ALL_PACKAGES",
            "adb shell pm grant $packageName android.permission.PACKAGE_USAGE_STATS",
            "adb shell pm grant $packageName android.permission.REQUEST_INSTALL_PACKAGES"
        )
    }

    fun copyAdbCommandsToClipboard(packageName: String): Boolean {
        val commands = getAdbGrantCommands(packageName).joinToString("\n")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("ADB Grant Commands", commands)
        clipboard.setPrimaryClip(clip)
        return true
    }

    fun getPermissionStatus(): Map<String, Boolean> {
        return mapOf(
            "MANAGE_EXTERNAL_STORAGE" to checkManageStorage(),
            "QUERY_ALL_PACKAGES" to checkQueryAllPackages(),
            "PACKAGE_USAGE_STATS" to checkUsageStats(),
            "REQUEST_INSTALL_PACKAGES" to checkInstallPackages()
        )
    }
}