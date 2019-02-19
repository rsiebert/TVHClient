package org.tvheadend.tvhclient.ui.features.search;

public interface SearchRequestInterface {

    void onSearchRequested(String query);

    boolean onSearchResultsCleared();

    String getQueryHint();
}
