package org.tvheadend.tvhclient.res

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards against the most common translation defect: a locale string keeps the English wording but
 * drops, renames or reorders the printf-style arguments (for example loses `%1$s`). That makes
 * `String.format` throw `IllegalFormatException` / `MissingFormatArgument` at runtime, so it must
 * never reach a release.
 *
 * Only keys that exist in both the default and the locale file are compared here. Completely
 * missing or extra keys are the job of lint's `MissingTranslation` / `ExtraTranslation`
 * (see `app/lint.xml`).
 *
 * The test reads the `strings.xml` files straight from the source tree rather than going through
 * the Android resource system, because it needs to inspect every locale at once - something the
 * merged resources of a single unit test do not expose.
 */
class StringResourcesTranslationTest {

    /** Locale qualifiers that ship translations, matching the `values-*` resource folders. */
    private val locales = listOf("cs", "de", "es", "fr", "hu", "it", "nl", "pl", "sk", "sv")

    private val resDir: File by lazy { locateResDir() }

    private val defaultStrings: Map<String, String> by lazy { readStrings("values/strings.xml") }

    @Test
    fun everyTranslatedStringKeepsTheDefaultFormatSpecifiers() {
        val mismatches = mutableListOf<String>()

        for (locale in locales) {
            val translated = readStrings("values-$locale/strings.xml")
            for ((key, defaultValue) in defaultStrings) {
                val translatedValue = translated[key] ?: continue
                val expected = placeholders(defaultValue)
                val actual = placeholders(translatedValue)
                if (expected != actual) {
                    mismatches += "values-$locale/$key: expected $expected but was $actual"
                }
            }
        }

        assertThat(mismatches).isEmpty()
    }

    /**
     * Extracts the printf placeholders of [text] as a position -> conversion-type list, so that
     * `%s` and `%1$s` are treated as equivalent and reordering of indexed arguments is allowed,
     * while a dropped argument, a changed type or a swap of two types is reported.
     */
    private fun placeholders(text: String): List<Pair<Int, Char>> {
        // Collapse escaped percent signs (`%%`) so they are not mistaken for format specifiers.
        val found = PLACEHOLDER.findAll(text.replace("%%", ""))
            .map { match ->
                match.groupValues[1].takeIf { it.isNotEmpty() }?.toInt() to match.groupValues[2][0]
            }
            .toList()

        val indexed = found.any { it.first != null }
        return if (indexed) {
            found.map { (index, conversion) ->
                val position = checkNotNull(index) { "mixed indexed and unindexed specifiers in: $text" }
                position to conversion
            }.sortedBy { it.first }
        } else {
            found.mapIndexed { position, (_, conversion) -> (position + 1) to conversion }
        }
    }

    private fun readStrings(relativePath: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // The files carry no DTD, but don't let the parser reach out to the network regardless.
            runCatching {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
        }.newDocumentBuilder().parse(File(resDir, relativePath))

        val nodes = document.getElementsByTagName("string")
        val result = LinkedHashMap<String, String>(nodes.length)
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            if (element.getAttribute("translatable") == "false") continue
            result[element.getAttribute("name")] = element.textContent
        }
        return result
    }

    /**
     * Resolves `<module>/src/main/res` from the Gradle test working directory (usually the module
     * directory, sometimes the repository root), walking up so the test does not depend on the
     * exact directory Gradle happens to pick.
     */
    private fun locateResDir(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            for (candidate in listOf(File(dir, "src/main/res"), File(dir, "app/src/main/res"))) {
                if (File(candidate, "values/strings.xml").isFile) return candidate
            }
            dir = dir.parentFile
        }
        error("Could not locate app/src/main/res from ${System.getProperty("user.dir")}")
    }

    private companion object {
        /** Matches a printf placeholder such as `%s`, `%1$d` or `%-5.2f`; a literal `%%` is ignored. */
        val PLACEHOLDER = Regex("""%(?:(\d+)[$])?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z])""")
    }
}
