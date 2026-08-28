package com.crawler.data.adb

data class AdbCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean
        get() = exitCode == 0
}
