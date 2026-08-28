package com.crawler.data.converter

import androidx.room.TypeConverter
import com.crawler.data.entity.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DayOfWeek
import java.lang.reflect.Type
import java.util.*

class Converters {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listStringType: Type = Types.newParameterizedType(List::class.java, String::class.java)
    private val mapStringStringType: Type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val listExtractionRuleType: Type = Types.newParameterizedType(List::class.java, ExtractionRuleEntity::class.java)
    private val listPostProcessorType: Type = Types.newParameterizedType(List::class.java, PostProcessorEntity::class.java)
    private val setStringType: Type = Types.newParameterizedType(Set::class.java, String::class.java)

    @TypeConverter
    fun listStringToJson(list: List<String>?): String = list?.let { moshi.adapter(listStringType).toJson(it) } ?: "[]"

    @TypeConverter
    fun jsonToListString(json: String): List<String> = moshi.adapter(listStringType).fromJson(json) ?: emptyList()

    @TypeConverter
    fun mapStringStringToJson(map: Map<String, String>?): String = map?.let { moshi.adapter(mapStringStringType).toJson(it) } ?: "{}"

    @TypeConverter
    fun jsonToMapStringString(json: String): Map<String, String> = moshi.adapter(mapStringStringType).fromJson(json) ?: emptyMap()

    @TypeConverter
    fun listExtractionRuleToJson(list: List<ExtractionRuleEntity>?): String = list?.let { moshi.adapter(listExtractionRuleType).toJson(it) } ?: "[]"

    @TypeConverter
    fun jsonToListExtractionRule(json: String): List<ExtractionRuleEntity> = moshi.adapter(listExtractionRuleType).fromJson(json) ?: emptyList()

    @TypeConverter
    fun listPostProcessorToJson(list: List<PostProcessorEntity>?): String = list?.let { moshi.adapter(listPostProcessorType).toJson(it) } ?: "[]"

    @TypeConverter
    fun jsonToListPostProcessor(json: String): List<PostProcessorEntity> = moshi.adapter(listPostProcessorType).fromJson(json) ?: emptyList()

    @TypeConverter
    fun setStringToJson(set: Set<String>?): String = set?.let { moshi.adapter(setStringType).toJson(it) } ?: "[]"

    @TypeConverter
    fun jsonToSetString(json: String): Set<String> = moshi.adapter(setStringType).fromJson(json)?.toSet() ?: emptySet()

    @TypeConverter
    fun instantToLong(instant: Instant?): Long = instant?.toEpochMilliseconds() ?: 0L

    @TypeConverter
    fun longToInstant(millis: Long): Instant? = if (millis > 0) Instant.fromEpochMilliseconds(millis) else null

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String = time?.toString() ?: ""

    @TypeConverter
    fun stringToLocalTime(str: String): LocalTime? = if (str.isNotBlank()) LocalTime.parse(str) else null

    @TypeConverter
    fun dayOfWeekToInt(day: DayOfWeek?): Int = day?.ordinal ?: -1

    @TypeConverter
    fun intToDayOfWeek(ordinal: Int): DayOfWeek? = if (ordinal >= 0) DayOfWeek.values()[ordinal] else null

    @TypeConverter
    fun selectorTypeToString(type: SelectorType): String = type.name

    @TypeConverter
    fun stringToSelectorType(name: String): SelectorType = SelectorType.valueOf(name)

    @TypeConverter
    fun multipleStrategyToString(strategy: MultipleStrategy): String = strategy.name

    @TypeConverter
    fun stringToMultipleStrategy(name: String): MultipleStrategy = MultipleStrategy.valueOf(name)

    @TypeConverter
    fun httpMethodToString(method: HttpMethod): String = method.name

    @TypeConverter
    fun stringToHttpMethod(name: String): HttpMethod = HttpMethod.valueOf(name)

    @TypeConverter
    fun bodyTypeToString(type: BodyType): String = type.name

    @TypeConverter
    fun stringToBodyType(name: String): BodyType = BodyType.valueOf(name)

    @TypeConverter
    fun scheduleTypeToString(type: ScheduleType): String = type.name

    @TypeConverter
    fun stringToScheduleType(name: String): ScheduleType = ScheduleType.valueOf(name)

    @TypeConverter
    fun waitConditionToString(condition: WaitCondition): String = condition.name

    @TypeConverter
    fun stringToWaitCondition(name: String): WaitCondition = WaitCondition.valueOf(name)

    @TypeConverter
    fun authTypeToString(type: AuthType): String = type.name

    @TypeConverter
    fun stringToAuthType(name: String): AuthType = AuthType.valueOf(name)

    @TypeConverter
    fun payloadFormatToString(format: PayloadFormat): String = format.name

    @TypeConverter
    fun stringToPayloadFormat(name: String): PayloadFormat = PayloadFormat.valueOf(name)

    @TypeConverter
    fun resultStatusToString(status: ResultStatus): String = status.name

    @TypeConverter
    fun stringToResultStatus(name: String): ResultStatus = ResultStatus.valueOf(name)

    @TypeConverter
    fun postProcessorDataTypeToString(type: PostProcessorEntity.DataType): String = type.name

    @TypeConverter
    fun stringToPostProcessorDataType(name: String): PostProcessorEntity.DataType = PostProcessorEntity.DataType.valueOf(name)
}