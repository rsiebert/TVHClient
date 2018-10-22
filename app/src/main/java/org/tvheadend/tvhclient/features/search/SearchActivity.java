package org.tvheadend.tvhclient.features.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.shared.BaseActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import timber.log.Timber;

public class SearchActivity extends BaseActivity implements ToolbarInterface, StartSearchInterface {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.misc_content_activity);
        MiscUtils.setLanguage(this);

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
        if (fragment != null && fragment.isAdded()) {
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

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }
}
