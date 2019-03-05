package org.tvheadend.tvhclient.ui.features.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.BaseActivity;
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.util.MiscUtils;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import timber.log.Timber;

public class SearchActivity extends BaseActivity implements StartSearchInterface {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.misc_content_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        handleIntent(getIntent());

        if (savedInstanceState == null) {
            // If a search was performed from a fragment the activity would call
            // the onSearchRequested(...) method of that fragment which will start
            // the SearchActivity (if implemented). Depending on the given search type
            // the corresponding fragment will be shown which will present the results.
            String type = getIntent().getStringExtra("type");
            Fragment fragment = null;
            switch (type) {
                case "program_guide":
                    fragment = new ProgramListFragment();
                    break;
            }

            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main, fragment)
                        .commit();
            }
        }
    }

    public void startSearch() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isVisible()) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            ((SearchRequestInterface) fragment).onSearchRequested(query);
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
            Timber.d("Saving suggestion " + query);
            suggestions.saveRecentQuery(query, null);
        }
    }
}
