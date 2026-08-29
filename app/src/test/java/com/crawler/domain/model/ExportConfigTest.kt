package com.crawler.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ExportConfigTest {

    @Test
    fun `resolveFields keeps normal fields and excludes sensitive when enabled`() {
        val fields = listOf("title", "url", "password", "API_KEY", "Secret", "price")
        val config = ExportConfig(format = ExportFormat.JSON, excludeSensitiveFields = true)
        val resolved = config.resolveFields(fields)
        assertThat(resolved).containsExactly("title", "url", "price")
    }

    @Test
    fun `resolveFields keeps sensitive fields when flag disabled`() {
        val fields = listOf("title", "password", "token")
        val config = ExportConfig(format = ExportFormat.JSON, excludeSensitiveFields = false)
        val resolved = config.resolveFields(fields)
        assertThat(resolved).containsExactly("title", "password", "token")
    }

    @Test
    fun `resolveFields applies includeFields intersection`() {
        val fields = listOf("a", "b", "c", "token")
        val config = ExportConfig(format = ExportFormat.JSON, includeFields = setOf("a", "c", "token"), excludeSensitiveFields = true)
        val resolved = config.resolveFields(fields)
        assertThat(resolved).containsExactly("a", "c")
    }

    @Test
    fun `sensitive patterns are case insensitive`() {
        // "api_key", "ApiKey", "API-KEY" all match
        val fields = listOf("api_key", "ApiKey", "API-KEY", "Authorization", "x")
        val config = ExportConfig(format = ExportFormat.JSON, excludeSensitiveFields = true)
        val resolved = config.resolveFields(fields)
        assertThat(resolved).containsExactly("x")
    }
}