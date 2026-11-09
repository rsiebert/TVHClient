package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Reads the changelog asset off the main thread.
 *
 * Replaces the loader that used to live inside `ChangeLogFragment`, which additionally parsed the
 * marker based text file into an HTML string; parsing is [ChangeLogParser]'s job now.
 */
class ChangeLogLoader(private val context: Context) {

    /**
     * Returns the changelog entries, or an empty list if the asset is missing or unreadable.
     */
    suspend fun load(): List<ChangeLogEntry> = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.mapCatching { ChangeLogParser.parse(it) }
            .getOrElse { throwable ->
                Timber.w(throwable, "Could not read the changelog asset $ASSET_NAME")
                emptyList()
            }
    }

    private companion object {
        const val ASSET_NAME = "changelog.json"
    }
}
