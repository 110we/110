package com.crawler.data.adb

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbClient @Inject constructor(
    private val context: Context
) {

    enum class Mode { SHIZUKU, ROOT, LOCAL }

    val isShizukuAvailable: Boolean
        get() = ShizukuProvider.isBinderAlive()

    val isShizukuInstalled: Boolean
        get() = runCatching {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        }.getOrDefault(false)

    fun openShizukuApp(): Boolean {
        return runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    val isShizukuAuthorized: Boolean
        get() = try {
            isShizukuAvailable &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }

    val isRootAvailable: Boolean
        get() = runCatching {
            val process = ProcessBuilder("which", "su").start()
            val output = process.inputStream.readBytes().toString(Charsets.UTF_8).trim()
            process.waitFor(3, TimeUnit.SECONDS)
            process.destroy()
            output.isNotBlank()
        }.getOrDefault(false)

    private val activeMode: Mode
        get() = when {
            isShizukuAuthorized -> Mode.SHIZUKU
            isRootAvailable -> Mode.ROOT
            else -> Mode.LOCAL
        }

    fun requestShizukuPermission(requestCode: Int) {
        if (!isShizukuAvailable) return
        runCatching {
            Shizuku.requestPermission(requestCode)
        }
    }

    suspend fun execute(command: String): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            when (activeMode) {
                Mode.SHIZUKU -> executeViaShizuku(command)
                Mode.ROOT -> executeViaRoot(command)
                Mode.LOCAL -> executeViaLocal(command)
            }
        }
    }

    suspend fun readFile(path: String): String? {
        val result = execute("cat \"$path\" 2>/dev/null || cat '$path'")
        return if (result.success) result.stdout.ifBlank { null } else null
    }

    suspend fun checkPermission(permission: String): Boolean {
        val result = execute("pm check-permission $permission ${context.packageName}")
        val granted = result.stdout.trim().equals("granted", ignoreCase = true)
        Timber.d("checkPermission $permission -> $granted (${result.stdout})")
        return granted
    }

    suspend fun grantPermission(permission: String): Boolean {
        val result = execute("pm grant ${context.packageName} $permission")
        Timber.d("grantPermission $permission -> exit=${result.exitCode} (${result.stderr})")
        return result.success
    }

    private fun executeViaShizuku(command: String): AdbCommandResult {
        return runCatching {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val stdout = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val stderr = process.errorStream.readBytes().toString(Charsets.UTF_8)
            val exitCode = process.waitFor()
            AdbCommandResult(exitCode, stdout.trim(), stderr.trim())
        }.getOrElse { e ->
            Timber.e(e, "Shizuku command failed: $command")
            AdbCommandResult(-1, "", e.message ?: "Shizuku error")
        }
    }

    private fun executeViaRoot(command: String): AdbCommandResult {
        return runCatching {
            val process = ProcessBuilder("su", "-c", command).start()
            val stdout = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val stderr = process.errorStream.readBytes().toString(Charsets.UTF_8)
            val exitCode = process.waitFor()
            AdbCommandResult(exitCode, stdout.trim(), stderr.trim())
        }.getOrElse { e ->
            Timber.e(e, "Root command failed: $command")
            AdbCommandResult(-1, "", e.message ?: "Root error")
        }
    }

    private fun executeViaLocal(command: String): AdbCommandResult {
        return runCatching {
            val process = ProcessBuilder("sh", "-c", command).start()
            val stdout = process.inputStream.readBytes().toString(Charsets.UTF_8)
            val stderr = process.errorStream.readBytes().toString(Charsets.UTF_8)
            val exitCode = process.waitFor()
            AdbCommandResult(exitCode, stdout.trim(), stderr.trim())
        }.getOrElse { e ->
            Timber.e(e, "Local command failed: $command")
            AdbCommandResult(-1, "", e.message ?: "Local error")
        }
    }
}
