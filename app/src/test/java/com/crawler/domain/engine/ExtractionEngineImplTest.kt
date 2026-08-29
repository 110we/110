package com.crawler.domain.engine

import com.crawler.domain.model.ExtractionRule
import com.crawler.domain.model.MultipleStrategy
import com.crawler.domain.model.PostProcessor
import com.crawler.domain.model.SelectorType
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ExtractionEngineImplTest {

    private val engine = ExtractionEngineImpl()

    private val html = """
        <html><body>
            <ul class="items">
                <li><a href="/a" class="title">  Alpha  </a></li>
                <li><a href="/b" class="title">Beta</a></li>
            </ul>
            <span id="price">42</span>
            <div class="box">Hello, World</div>
        </body></html>
    """.trimIndent()

    @Test
    fun `CSS first match extracts text`() {
        val rules = listOf(ExtractionRule("title", SelectorType.CSS, ".title"))
        val result = engine.extract(html, rules)
        assertThat(result["title"]).isEqualTo("Alpha")
    }

    @Test
    fun `CSS ALL_ARRAY returns all matches`() {
        val rules = listOf(
            ExtractionRule("titles", SelectorType.CSS, ".title", multiple = MultipleStrategy.ALL_ARRAY)
        )
        val result = engine.extract(html, rules)
        assertThat(result["titles"] as List<*>).containsExactly("Alpha", "Beta")
    }

    @Test
    fun `CSS JOIN delimits values`() {
        val rules = listOf(
            ExtractionRule("titles", SelectorType.CSS, ".title", multiple = MultipleStrategy.JOIN, joinDelimiter = "|")
        )
        val result = engine.extract(html, rules)
        assertThat(result["titles"]).isEqualTo("Alpha|Beta")
    }

    @Test
    fun `attribute extraction reads href`() {
        val rules = listOf(ExtractionRule("link", SelectorType.CSS, "a", attribute = "href"))
        val result = engine.extract(html, rules)
        assertThat(result["link"]).isEqualTo("/a")
    }

    @Test
    fun `trim post processor strips whitespace`() {
        val rules = listOf(
            ExtractionRule("title", SelectorType.CSS, ".title", postProcessors = listOf(PostProcessor.Trim()))
        )
        val result = engine.extract(html, rules)
        assertThat(result["title"]).isEqualTo("Alpha")
    }

    @Test
    fun `regex replace post processor`() {
        val rules = listOf(
            ExtractionRule("cleaned", SelectorType.CSS, ".box", postProcessors = listOf(PostProcessor.RegexReplace("World", "Kotlin")))
        )
        val result = engine.extract(html, rules)
        assertThat(result["cleaned"]).isEqualTo("Hello, Kotlin")
    }

    @Test
    fun `empty selection returns null for FIRST`() {
        val rules = listOf(ExtractionRule("missing", SelectorType.CSS, ".nonexistent"))
        val result = engine.extract(html, rules)
        assertThat(result.containsKey("missing")).isFalse()
    }

    @Test
    fun `regex extraction captures group`() {
        val rules = listOf(ExtractionRule("price", SelectorType.REGEX, """\d+"""))
        val result = engine.extract(html, rules)
        assertThat(result["price"]).isEqualTo("42")
    }

    @Test
    fun `malformed css does not throw`() {
        val rules = listOf(ExtractionRule("bad", SelectorType.CSS, "::]]["))
        val result = engine.extract(html, rules)
        assertThat(result.containsKey("bad")).isFalse()
    }

    @Test
    fun `regex ALL_ARRAY captures all groups`() {
        val rules = listOf(
            ExtractionRule("nums", SelectorType.REGEX, """(\d+)""", multiple = MultipleStrategy.ALL_ARRAY)
        )
        val result = engine.extract(html, rules)
        assertThat(result["nums"] as List<*>).containsExactly("42")
    }

    @Test
    fun `trim removes surrounding whitespace then regex replace`() {
        val rules = listOf(
            ExtractionRule(
                "t",
                SelectorType.CSS,
                ".title",
                postProcessors = listOf(PostProcessor.Trim(), PostProcessor.RegexReplace("Alpha", "A"))
            )
        )
        val result = engine.extract(html, rules)
        assertThat(result["t"]).isEqualTo("A")
    }

    @Test
    fun `empty html returns empty extras instead of throwing`() {
        val rules = listOf(ExtractionRule("x", SelectorType.CSS, "p"))
        val result = engine.extract("", rules)
        assertThat(result.containsKey("x")).isFalse()
    }

    @Test
    fun `extractJson with regex extracts value`() {
        val json = """{"user":"alice","age":"30"}"""
        val rules = listOf(ExtractionRule("age", SelectorType.REGEX, "\"(age)\":\"?(\\d+)\"?"))
        val result = engine.extractJson(json, rules)
        assertThat(result["age"]).isNotNull()
    }
}