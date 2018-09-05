package org.tvheadend.tvhclient.features.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.channels.ChannelListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.shared.BaseActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import timber.log.Timber;

public class SearchActivity extends BaseActivity implements ToolbarInterface {

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
                case "channels":
                    fragment = new ChannelListFragment();
                    break;
                case "program_guide":
                case "programs":
                    fragment = new ProgramListFragment();
                    break;
                case "completed_recordings":
                    fragment = new CompletedRecordingListFragment();
                    break;
                case "scheduled_recordings":
                    fragment = new ScheduledRecordingListFragment();
                    break;
                case "removed_recordings":
                    fragment = new RemovedRecordingListFragment();
                    break;
                case "failed_recordings":
                    fragment = new FailedRecordingListFragment();
                    break;
                case "series_recordings":
                    fragment = new SeriesRecordingListFragment();
                    break;
                case "timer_recordings":
                    fragment = new TimerRecordingListFragment();
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
