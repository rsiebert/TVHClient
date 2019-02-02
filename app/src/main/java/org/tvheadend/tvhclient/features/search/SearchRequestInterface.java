package org.tvheadend.tvhclient.features.search;

public interface SearchRequestInterface {

    void onSearchRequested(String query);

    boolean onSearchResultsCleared();

    String getQueryHint();
}
