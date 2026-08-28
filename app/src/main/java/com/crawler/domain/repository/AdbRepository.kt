package com.crawler.domain.repository

import com.crawler.data.adb.AdbCommandResult

interface AdbRepository {

    val isShizukuAvailable: Boolean

    val isShizukuAuthorized: Boolean

    val isRootAvailable: Boolean

    val isShizukuInstalled: Boolean

    fun openShizukuApp(): Boolean

    suspend fun execute(command: String): AdbCommandResult

    suspend fun readFile(path: String): String?

    suspend fun checkPermission(permission: String): Boolean

    suspend fun grantPermission(permission: String): Boolean
}
