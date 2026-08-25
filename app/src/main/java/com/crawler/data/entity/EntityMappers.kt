package com.crawler.data.entity

import com.crawler.domain.model.*

fun CrawlTaskEntity.toDomain(): CrawlTask {
    return CrawlTask(
        id = id,
        name = name,
        baseUrls = baseUrls,
        urlPatterns = com.crawler.domain.model.UrlPatterns(
            includePatterns = urlPatterns.includePatterns,
            excludePatterns = urlPatterns.excludePatterns,
            maxDepth = urlPatterns.maxDepth,
            maxPages = urlPatterns.maxPages
        ),
        extractionRules = extractionRules.map { it.toDomain() },
        requestConfig = requestConfig.toDomain(),
        scheduleConfig = scheduleConfig?.toDomain(),
        jsRenderingConfig = jsRenderingConfig?.toDomain(),
        syncConfig = syncConfig?.toDomain(),
        createdAt = kotlinx.datetime.Instant.ofEpochMillisecond(createdAt),
        updatedAt = kotlinx.datetime.Instant.ofEpochMillisecond(updatedAt)
    )
}

fun ExtractionRuleEntity.toDomain(): ExtractionRule {
    return ExtractionRule(
        fieldName = fieldName,
        selectorType = SelectorType.valueOf(selectorType.name),
        expression = expression,
        attribute = attribute,
        multiple = MultipleStrategy.valueOf(multiple.name),
        joinDelimiter = joinDelimiter,
        postProcessors = postProcessors.map { it.toDomain() }
    )
}

fun PostProcessorEntity.toDomain(): PostProcessor {
    return when (this) {
        is PostProcessorEntity.Trim -> PostProcessor.Trim(enabled)
        is PostProcessorEntity.RegexReplace -> PostProcessor.RegexReplace(pattern, replacement)
        is PostProcessorEntity.TypeConversion -> PostProcessor.TypeConversion(
            PostProcessor.DataType.valueOf(targetType.name)
        )
    }
}

fun RequestConfigEntity.toDomain(): RequestConfig {
    return RequestConfig(
        method = HttpMethod.valueOf(method.name),
        headers = headers,
        cookies = cookies,
        body = body,
        bodyType = BodyType.valueOf(bodyType.name),
        timeoutSeconds = timeoutSeconds,
        followRedirects = followRedirects,
        maxRedirects = maxRedirects,
        userAgent = userAgent
    )
}

fun ScheduleConfigEntity.toDomain(): ScheduleConfig {
    return ScheduleConfig(
        type = ScheduleType.valueOf(type.name),
        cronExpression = cronExpression,
        timeOfDay = timeOfDay,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        enabled = enabled
    )
}

fun JsRenderingConfigEntity.toDomain(): JsRenderingConfig {
    return JsRenderingConfig(
        enabled = enabled,
        waitCondition = WaitCondition.valueOf(waitCondition.name),
        waitSelector = waitSelector,
        waitScript = waitScript,
        timeoutSeconds = timeoutSeconds,
        blockResources = blockResources
    )
}

fun SyncConfigEntity.toDomain(): SyncConfig {
    return SyncConfig(
        enabled = enabled,
        endpoint = endpoint,
        authType = AuthType.valueOf(authType.name),
        credentials = EncryptedCredentials(credentials.username, credentials.password),
        payloadFormat = PayloadFormat.valueOf(payloadFormat.name),
        syncOnComplete = syncOnComplete
    )
}

fun CrawlResultEntity.toDomain(): CrawlResult {
    val data = try {
        kotlinx.serialization.json.Json.decodeFromString(
            kotlinx.serialization.typeOf<Map<String, Any>>(), extractedData
        )
    } catch (e: Exception) {
        emptyMap<String, Any>()
    }
    return CrawlResult(
        id = id,
        taskId = taskId,
        url = url,
        data = data,
        status = ResultStatus.valueOf(status.name),
        errorMessage = errorMessage,
        crawledAt = kotlinx.datetime.Instant.ofEpochMillisecond(crawledAt),
        syncedAt = syncedAt?.let { kotlinx.datetime.Instant.ofEpochMillisecond(it) }
    )
}

fun CrawlTask.toEntity(): CrawlTaskEntity {
    return CrawlTaskEntity(
        id = id,
        name = name,
        baseUrls = baseUrls,
        urlPatterns = UrlPatternsEntity(
            includePatterns = urlPatterns.includePatterns,
            excludePatterns = urlPatterns.excludePatterns,
            maxDepth = urlPatterns.maxDepth,
            maxPages = urlPatterns.maxPages
        ),
        extractionRules = extractionRules.map { it.toEntity() },
        requestConfig = requestConfig.toEntity(),
        scheduleConfig = scheduleConfig?.toEntity(),
        jsRenderingConfig = jsRenderingConfig?.toEntity(),
        syncConfig = syncConfig?.toEntity(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

fun ExtractionRule.toEntity(): ExtractionRuleEntity {
    return ExtractionRuleEntity(
        fieldName = fieldName,
        selectorType = SelectorType.valueOf(selectorType.name),
        expression = expression,
        attribute = attribute,
        multiple = MultipleStrategy.valueOf(multiple.name),
        joinDelimiter = joinDelimiter,
        postProcessors = postProcessors.map { it.toEntity() }
    )
}

fun PostProcessor.toEntity(): PostProcessorEntity {
    return when (this) {
        is PostProcessor.Trim -> PostProcessorEntity.Trim(enabled)
        is PostProcessor.RegexReplace -> PostProcessorEntity.RegexReplace(pattern, replacement)
        is PostProcessor.TypeConversion -> PostProcessorEntity.TypeConversion(
            PostProcessorEntity.DataType.valueOf(targetType.name)
        )
    }
}

fun RequestConfig.toEntity(): RequestConfigEntity {
    return RequestConfigEntity(
        method = HttpMethod.valueOf(method.name),
        headers = headers,
        cookies = cookies,
        body = body,
        bodyType = BodyType.valueOf(bodyType.name),
        timeoutSeconds = timeoutSeconds,
        followRedirects = followRedirects,
        maxRedirects = maxRedirects,
        userAgent = userAgent
    )
}

fun ScheduleConfig.toEntity(): ScheduleConfigEntity {
    return ScheduleConfigEntity(
        type = ScheduleType.valueOf(type.name),
        cronExpression = cronExpression,
        timeOfDay = timeOfDay,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        enabled = enabled
    )
}

fun JsRenderingConfig.toEntity(): JsRenderingConfigEntity {
    return JsRenderingConfigEntity(
        enabled = enabled,
        waitCondition = WaitCondition.valueOf(waitCondition.name),
        waitSelector = waitSelector,
        waitScript = waitScript,
        timeoutSeconds = timeoutSeconds,
        blockResources = blockResources
    )
}

fun SyncConfig.toEntity(): SyncConfigEntity {
    return SyncConfigEntity(
        enabled = enabled,
        endpoint = endpoint,
        authType = AuthType.valueOf(authType.name),
        credentials = EncryptedCredentialsEntity(credentials.username, credentials.password),
        payloadFormat = PayloadFormat.valueOf(payloadFormat.name),
        syncOnComplete = syncOnComplete
    )
}