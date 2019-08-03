package org.tvheadend.tvhclient.ui.features.search

import android.content.SearchRecentSuggestionsProvider

class SuggestionProvider : SearchRecentSuggestionsProvider() {

    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "org.tvheadend.tvhclient.ui.features.search.SuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}
