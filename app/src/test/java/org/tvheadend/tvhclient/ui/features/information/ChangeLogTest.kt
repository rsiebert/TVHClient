package org.tvheadend.tvhclient.ui.features.information

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.BuildConfig

/**
 * Covers the changelog data, its parser and the logic that decides which versions are shown.
 *
 * The changelog used to be a plain text file with a private marker grammar that was parsed into an
 * HTML string by hand. Nothing guarded it, which is how the two quirks it had (the "was shown"
 * marker only being written on back, and an endless spinner when the current version had no entry)
 * went unnoticed. Robolectric is used because the parser is built on `org.json`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChangeLogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Reads the changelog the way the changelog screen does, i.e. through the real asset. */
    private fun loadEntries(): List<ChangeLogEntry> = runBlocking { ChangeLogLoader(context).load() }

    @Test
    fun everyBuildVersionHasAChangelogEntry() {
        val versions = loadEntries().map { it.version }

        assertThat(versions).contains(BuildConfig.VERSION_NAME)
    }

    @Test
    fun theNewestEntryIsTheCurrentBuildVersion() {
        val entries = loadEntries()

        assertThat(entries).isNotEmpty()
        assertThat(entries.first().version).isEqualTo(BuildConfig.VERSION_NAME)
    }

    @Test
    fun everyEntryHasAVersionADateAndAtLeastOneItem() {
        val entries = loadEntries()

        assertThat(entries).isNotEmpty()
        entries.forEach { entry ->
            assertThat(entry.version).isNotEmpty()
            assertThat(entry.date).isNotEmpty()
            // A version without items would render a heading with nothing below it.
            assertThat(entry.items).isNotEmpty()
        }
    }

    @Test
    fun everyVersionAppearsOnlyOnce() {
        val versions = loadEntries().map { it.version }

        assertThat(versions).containsNoDuplicates()
    }

    @Test
    fun theParserReturnsNothingForMalformedJson() {
        assertThat(ChangeLogParser.parse("not json at all")).isEmpty()
    }

    @Test
    fun theParserSkipsEntriesWithoutAVersion() {
        val json = """[{"date":"01.01.2020","items":["change"]},{"version":"1.0.0","date":"02.01.2020","items":["change"]}]"""

        assertThat(ChangeLogParser.parse(json).map { it.version }).containsExactly("1.0.0")
    }

    @Test
    fun theParserToleratesAMissingDateAndItemList() {
        val entry = ChangeLogParser.parse("""[{"version":"1.0.0"}]""").single()

        assertThat(entry.date).isEmpty()
        assertThat(entry.items).isEmpty()
    }

    private val sampleEntries = listOf(
        ChangeLogEntry("3.0.0", "07.11.2026", listOf("newest")),
        ChangeLogEntry("2.4.8", "23.11.2023", listOf("middle")),
        ChangeLogEntry("2.4.7", "03.01.2022", listOf("oldest"))
    )

    @Test
    fun theFullChangeLogShowsEveryEntry() {
        assertThat(selectEntries(sampleEntries, "2.4.8", showFull = true)).isEqualTo(sampleEntries)
    }

    @Test
    fun aFreshInstallShowsEveryEntry() {
        assertThat(selectEntries(sampleEntries, "", showFull = false)).isEqualTo(sampleEntries)
    }

    @Test
    fun anUpdateShowsTheNewerEntriesIncludingTheLastSeenOne() {
        val selected = selectEntries(sampleEntries, "2.4.8", showFull = false)

        assertThat(selected.map { it.version }).containsExactly("3.0.0", "2.4.8").inOrder()
    }

    @Test
    fun anUnknownLastSeenVersionShowsNothing() {
        assertThat(selectEntries(sampleEntries, "9.9.9", showFull = false)).isEmpty()
    }

    @Test
    fun theRowsPutTheHeadingBeforeItsItems() {
        val entries = listOf(ChangeLogEntry("1.0.0", "01.01.2020", listOf("first", "second")))

        val rows = toRows(entries)

        assertThat(rows).hasSize(3)
        assertThat(rows[0]).isEqualTo(ChangeLogRow.Header("1.0.0", "01.01.2020"))
        assertThat(rows[1]).isEqualTo(ChangeLogRow.Item("first"))
        assertThat(rows[2]).isEqualTo(ChangeLogRow.Item("second"))
    }
}
