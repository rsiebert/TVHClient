package org.tvheadend.tvhclient.ui.features.search

interface SearchRequestInterface {

    fun getQueryHint(): String

    fun onSearchRequested(query: String)

    fun onSearchResultsCleared(): Boolean
}
