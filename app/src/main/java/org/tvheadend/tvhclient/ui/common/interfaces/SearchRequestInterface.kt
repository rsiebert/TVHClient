package org.tvheadend.tvhclient.ui.common.interfaces

interface SearchRequestInterface {

    fun getQueryHint(): String

    fun onSearchRequested(query: String)

    fun onSearchResultsCleared()
}
