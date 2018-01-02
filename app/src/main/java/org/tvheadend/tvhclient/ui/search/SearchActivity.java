package org.tvheadend.tvhclient.ui.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.local.SuggestionProvider;
import org.tvheadend.tvhclient.ui.NavigationActivity;
import org.tvheadend.tvhclient.ui.progams.ProgramListFragment;

public class SearchActivity extends NavigationActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());

        if (savedInstanceState == null) {

            // Example: If an initial search was performed from the ChannelListFragment its
            // Activity would call the onSearchRequested(...) method of the ChannelListFragment
            // which then starts the SearchActivity. It will show the ProgramListFragment with
            // the results.

            // Example 2: If an advanced search dialog was performed from the NavigationActivity
            // all relevant information is directly passed as a bundle to the SearchActivity.
            // It will check the query and forward the bundle information to the relevant
            // fragment that shall show the results.

            // Get the query from the intent. All other information is included in the bundle
            // which was added in the fragment that implemented the onSearchRequested method.
            //String query = getIntent().getStringExtra(SearchManager.QUERY);

            // Analyze the query for special words that determine what the
            // user wants to search for. If nothing was passed except text
            // then assume he wants to search for programs

            String type = getIntent().getStringExtra("type");
            Fragment fragment = null;
            switch (type) {
                case "programs":
                    fragment = new ProgramListFragment();
                    break;
                case "recordings":
                    fragment = new ProgramListFragment();
                    break;
            }
            if (fragment != null) {
                fragment.setArguments(getIntent().getBundleExtra(SearchManager.APP_DATA));
                getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // Save the query so it can be shown again.
            SearchRecentSuggestions suggestions =
                    new SearchRecentSuggestions(this,
                            SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
        }
    }
}
