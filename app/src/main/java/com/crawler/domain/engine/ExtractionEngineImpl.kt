package com.crawler.domain.engine

import com.crawler.domain.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.regex.Pattern

class ExtractionEngineImpl : ExtractionEngine {

    override fun extract(html: String, rules: List<ExtractionRule>): Map<String, Any> {
        val doc = Jsoup.parse(html)
        return extractFromDocument(doc, rules)
    }

    override fun extractJson(json: String, rules: List<ExtractionRule>): Map<String, Any> {
        // For JSON, we only support REGEX and XPath (via JSON path)
        // Using a simple approach: convert JSON to string and apply regex
        val result = mutableMapOf<String, Any>()
        for (rule in rules) {
            when (rule.selectorType) {
                SelectorType.REGEX -> {
                    val value = extractByRegex(json, rule)
                    value?.let { result[rule.fieldName] = it }
                }
                SelectorType.XPATH -> {
                    // Could use JSON path library here
                    val value = extractByRegex(json, rule) // fallback
                    value?.let { result[rule.fieldName] = it }
                }
                SelectorType.CSS -> {
                    // CSS not applicable for JSON
                    val value = when (rule.multiple) {
                        MultipleStrategy.ALL_ARRAY -> emptyList<Any>()
                        MultipleStrategy.JOIN -> ""
                        else -> null
                    }
                    value?.let { result[rule.fieldName] = it }
                }
            }
        }
        return result
    }

    override fun testRule(rule: ExtractionRule, sampleHtml: String): ExtractionTestResult {
        return try {
            val doc = Jsoup.parse(sampleHtml)
            val value = extractSingleRule(doc, rule)
            ExtractionTestResult(true, value, null)
        } catch (e: Exception) {
            ExtractionTestResult(false, null, e.message)
        }
    }

    private fun extractFromDocument(doc: Document, rules: List<ExtractionRule>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (rule in rules) {
            val value = extractSingleRule(doc, rule)
            value?.let { result[rule.fieldName] = it }
        }
        return result
    }

    private fun extractSingleRule(doc: Document, rule: ExtractionRule): Any? {
        val elements = when (rule.selectorType) {
            SelectorType.CSS -> selectByCss(doc, rule.expression)
            SelectorType.XPATH -> selectByXPath(doc, rule.expression)
            SelectorType.REGEX -> return extractByRegex(doc.html(), rule)
        }

        if (elements.isEmpty()) {
            return when (rule.multiple) {
                MultipleStrategy.ALL_ARRAY -> emptyList<Any>()
                MultipleStrategy.JOIN -> ""
                else -> null
            }
        }

        val values = elements.map { extractAttribute(it, rule.attribute ?: "text") }
            .filterNotNull()
            .map { applyPostProcessors(it, rule.postProcessors) }

        return when (rule.multiple) {
            MultipleStrategy.FIRST -> values.firstOrNull()
            MultipleStrategy.ALL_ARRAY -> values
            MultipleStrategy.JOIN -> values.joinToString(rule.joinDelimiter)
        }
    }

    private fun selectByCss(doc: Document, expression: String): Elements {
        return try {
            doc.select(expression)
        } catch (e: Exception) {
            Elements()
        }
    }

    private fun selectByXPath(doc: Document, expression: String): Elements {
        // Using a simple XPath implementation via jsoup's pseudo-selectors
        // For full XPath support, would need a dedicated library
        return try {
            // Jsoup doesn't support XPath natively, using a workaround
            // In production, use com.github.wnameless:xpath or similar
            val elements = Elements()
            // Simple XPath to CSS conversion for common cases
            val css = xpathToCss(expression)
            doc.select(css)
        } catch (e: Exception) {
            Elements()
        }
    }

    private fun xpathToCss(xpath: String): String {
        // Very basic XPath to CSS conversion
        return xpath
            .replace("//", " ")
            .replace("/", " > ")
            .replace("@class", "[class]")
            .replace("@id", "[id]")
            .replace("@href", "[href]")
            .replace("@src", "[src]")
    }

    private fun extractAttribute(element: Element, attribute: String): String? {
        return when (attribute) {
            "text" -> element.text()
            "html" -> element.html()
            "href" -> element.attr("href")
            "src" -> element.attr("src")
            else -> element.attr(attribute)
        }
    }

    private fun extractByRegex(text: String, rule: ExtractionRule): Any? {
        val pattern = Pattern.compile(rule.expression)
        val matcher = pattern.matcher(text)
        val matches = mutableListOf<String>()

        while (matcher.find()) {
            val groupCount = matcher.groupCount()
            if (groupCount > 0) {
                for (i in 1..groupCount) {
                    val group = matcher.group(i)
                    if (group != null) matches.add(group)
                }
            } else {
                matches.add(matcher.group())
            }
        }

        if (matches.isEmpty()) {
            return when (rule.multiple) {
                MultipleStrategy.ALL_ARRAY -> emptyList<Any>()
                MultipleStrategy.JOIN -> ""
                else -> null
            }
        }

        val processed = matches.map { applyPostProcessors(it, rule.postProcessors) }

        return when (rule.multiple) {
            MultipleStrategy.FIRST -> processed.firstOrNull()
            MultipleStrategy.ALL_ARRAY -> processed
            MultipleStrategy.JOIN -> processed.joinToString(rule.joinDelimiter)
        }
    }

    private fun applyPostProcessors(value: String, processors: List<PostProcessor>): String {
        var result = value
        for (processor in processors) {
            result = when (processor) {
                is PostProcessor.Trim -> if (processor.enabled) result.trim() else result
                is PostProcessor.RegexReplace -> result.replace(Regex(processor.pattern), processor.replacement)
                is PostProcessor.TypeConversion -> convertType(result, processor.targetType)
            }
        }
        return result
    }

    private fun convertType(value: String, targetType: PostProcessor.DataType): String {
        return try {
            when (targetType) {
                PostProcessor.DataType.INTEGER -> value.toInt().toString()
                PostProcessor.DataType.LONG -> value.toLong().toString()
                PostProcessor.DataType.DOUBLE -> value.toDouble().toString()
                PostProcessor.DataType.BOOLEAN -> when {
                    value.lowercase() in listOf("true", "1", "yes", "on") -> "true"
                    value.lowercase() in listOf("false", "0", "no", "off") -> "false"
                    else -> value
                }
                PostProcessor.DataType.DATE -> {
                    // Try to parse various date formats
                    val formats = listOf(
                        "yyyy-MM-dd",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS",
                        "yyyy/MM/dd",
                        "dd/MM/yyyy",
                        "MM/dd/yyyy"
                    )
                    var parsed: java.time.LocalDateTime? = null
                    for (format in formats) {
                        try {
                            parsed = java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern(format))
                            break
                        } catch (e: Exception) {
                            // Try next format
                        }
                    }
                    parsed?.toString() ?: value
                }
                else -> value
            }
        } catch (e: Exception) {
            value
        }
    }
}