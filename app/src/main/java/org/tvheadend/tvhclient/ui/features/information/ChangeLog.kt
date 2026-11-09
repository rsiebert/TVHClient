package org.tvheadend.tvhclient.ui.features.information

import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber

/**
 * One released application version as listed in the changelog.
 */
data class ChangeLogEntry(
    val version: String,
    val date: String,
    val items: List<String>
)

/**
 * A single row of the changelog list.
 */
sealed interface ChangeLogRow {

    /**
     * The heading that introduces the items of the version it belongs to.
     */
    data class Header(val version: String, val date: String) : ChangeLogRow

    /**
     * A single change of the version the enclosing [Header] belongs to.
     */
    data class Item(val text: String) : ChangeLogRow
}

/**
 * Reads the changelog from its JSON asset.
 *
 * The changelog used to be a plain text file with a private marker grammar (`$` version, `%` title,
 * `_` date, `*` bullet, `#` numbered item, `!` free text) that was parsed into an HTML string by
 * hand and rendered in a `WebView`. It is structured data now, so parsing is limited to mapping the
 * JSON onto [ChangeLogEntry]: no marker grammar, no string building and nothing that has to be
 * escaped to stay valid markup.
 */
object ChangeLogParser {

    /**
     * Maps [json] onto the changelog entries, keeping the order of the file (newest version first).
     *
     * Entries without a version are skipped and a missing date or item list is tolerated, so a
     * partially broken asset degrades instead of taking the screen down. Completely malformed input
     * yields an empty list for the same reason.
     */
    fun parse(json: String): List<ChangeLogEntry> {
        return try {
            val entries = JSONArray(json)
            (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val version = entry.optString("version")
                if (version.isEmpty()) {
                    return@mapNotNull null
                }

                val itemsJson = entry.optJSONArray("items")
                val items = if (itemsJson == null) {
                    emptyList()
                } else {
                    (0 until itemsJson.length()).mapNotNull { itemIndex ->
                        itemsJson.optString(itemIndex).takeIf { it.isNotEmpty() }
                    }
                }
                ChangeLogEntry(version, entry.optString("date"), items)
            }
        } catch (e: JSONException) {
            Timber.w(e, "Could not parse the changelog")
            emptyList()
        }
    }
}

/**
 * Returns the entries that the changelog screen has to show.
 *
 * The changelog is ordered newest first and [lastSeenVersion] is the version the user has already
 * read the changelog for, so everything up to and including that entry is what the user has not
 * seen yet. An empty [lastSeenVersion] means the changelog was never shown, which happens on a
 * fresh install, and then everything is returned. A version that does not appear in the changelog
 * yields nothing rather than dumping the complete log; that should not happen because
 * `ChangeLogTest` asserts the current build version has an entry.
 */
fun selectEntries(
    entries: List<ChangeLogEntry>,
    lastSeenVersion: String,
    showFull: Boolean
): List<ChangeLogEntry> {
    if (showFull || lastSeenVersion.isBlank()) {
        return entries
    }
    val lastSeenIndex = entries.indexOfFirst { it.version == lastSeenVersion }
    return if (lastSeenIndex >= 0) {
        entries.subList(0, lastSeenIndex + 1).toList()
    } else {
        emptyList()
    }
}

/**
 * Flattens [entries] into the heading and item rows the adapter renders.
 */
fun toRows(entries: List<ChangeLogEntry>): List<ChangeLogRow> {
    val rows = ArrayList<ChangeLogRow>()
    for (entry in entries) {
        rows.add(ChangeLogRow.Header(entry.version, entry.date))
        entry.items.mapTo(rows) { ChangeLogRow.Item(it) }
    }
    return rows
}
