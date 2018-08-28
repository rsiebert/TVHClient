package org.tvheadend.tvhclient.features.shared;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailabilityChangedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.features.shared.receivers.NetworkStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;

import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity implements NetworkStatusReceiverCallback, NetworkStatusInterface, EpgSyncStatusCallback, SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {

    private MenuItem searchMenuItem;
    private SearchView searchView;
    private NetworkStatusReceiver networkStatusReceiver;
    private boolean isNetworkAvailable;
    private ServiceStatusReceiver serviceStatusReceiver;
    private SnackbarMessageReceiver snackbarMessageReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("start");
        isNetworkAvailable = false;
        networkStatusReceiver = new NetworkStatusReceiver(this);
        serviceStatusReceiver = new ServiceStatusReceiver(this);
        snackbarMessageReceiver = new SnackbarMessageReceiver(this);
        Timber.d("end");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.d("start");
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, new IntentFilter(SnackbarMessageReceiver.ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStatusReceiver, new IntentFilter(ServiceStatusReceiver.ACTION));
        registerReceiver(networkStatusReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        Timber.d("end");
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("start");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStatusReceiver);
        unregisterReceiver(networkStatusReceiver);
        Timber.d("end");
    }

    @Override
    public void onNetworkStatusChanged(boolean isNetworkAvailable) {
        onNetworkAvailabilityChanged(isNetworkAvailable);
        if (!isNetworkAvailable && getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), "No network available.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onNetworkAvailabilityChanged(boolean isAvailable) {
        if (isAvailable) {
            Timber.d("Network is available");
            if (!isNetworkAvailable) {
                Timber.d("Network changed from offline to online, starting service");
                startService(new Intent(this, EpgSyncService.class));
            }
        } else {
            Timber.d("Network is not available anymore, stopping service");
            stopService(new Intent(this, EpgSyncService.class));
        }
        isNetworkAvailable = isAvailable;

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof NetworkAvailabilityChangedInterface) {
            ((NetworkAvailabilityChangedInterface) fragment).onNetworkAvailabilityChanged(isAvailable);
        }

        fragment = getSupportFragmentManager().findFragmentById(R.id.details);
        if (fragment != null && fragment instanceof NetworkAvailabilityChangedInterface) {
            ((NetworkAvailabilityChangedInterface) fragment).onNetworkAvailabilityChanged(isAvailable);
        }
        Timber.d("Network availability changed, invalidating menu");
        invalidateOptionsMenu();
    }

    @Override
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    @Override
    public void onEpgTaskStateChanged(EpgSyncTaskState state) {
        Timber.d("Epg task state changed, message is " + state.getMessage());
        switch (state.getState()) {
            case FAILED:
                if (getCurrentFocus() != null) {
                    Snackbar.make(getCurrentFocus(), state.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                onNetworkAvailabilityChanged(false);
                break;

            case CONNECTING:
            case CONNECTED:
                if (getCurrentFocus() != null) {
                    Snackbar.make(getCurrentFocus(), state.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchMenuItem = menu.findItem(R.id.menu_search);
            searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(this);
            searchView.setOnSuggestionListener(this);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Answers.getInstance().logSearch(new SearchEvent().putQuery(query));

        searchMenuItem.collapseActionView();
        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isAdded() && fragment instanceof SearchRequestInterface) {
            ((SearchRequestInterface) fragment).onSearchRequested(query);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        searchMenuItem.collapseActionView();
        // Set the search query and return true so that the onQueryTextSubmit
        // is called. This is required to pass additional data to the search activity
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
        String suggestion = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        searchView.setQuery(suggestion, true);
        return true;
    }
}
