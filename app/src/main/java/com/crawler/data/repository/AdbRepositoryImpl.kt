package com.crawler.data.repository

import com.crawler.data.adb.AdbClient
import com.crawler.data.adb.AdbCommandResult
import com.crawler.domain.repository.AdbRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbRepositoryImpl @Inject constructor(
    private val adbClient: AdbClient
) : AdbRepository {

    override val isShizukuAvailable: Boolean
        get() = adbClient.isShizukuAvailable

    override val isShizukuAuthorized: Boolean
        get() = adbClient.isShizukuAuthorized

    override val isRootAvailable: Boolean
        get() = adbClient.isRootAvailable

    override val isShizukuInstalled: Boolean
        get() = adbClient.isShizukuInstalled

    override fun openShizukuApp(): Boolean {
        return adbClient.openShizukuApp()
    }

    override suspend fun execute(command: String): AdbCommandResult {
        return adbClient.execute(command)
    }

    override suspend fun readFile(path: String): String? {
        return adbClient.readFile(path)
    }

    override suspend fun checkPermission(permission: String): Boolean {
        return adbClient.checkPermission(permission)
    }

    override suspend fun grantPermission(permission: String): Boolean {
        return adbClient.grantPermission(permission)
    }
}
