package org.tvheadend.tvhclient.features.startup;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusReceiver;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.navigation.NavigationActivity;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.features.shared.MenuUtils;
import org.tvheadend.tvhclient.utils.NetworkUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

// TODO add nice background image
// TODO use translated strings

public class StartupFragment extends Fragment implements EpgSyncStatusCallback {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.state)
    TextView stateTextView;
    @BindView(R.id.details)
    TextView detailsTextView;
    @BindView(R.id.add_connection_fab)
    FloatingActionButton addConnectionFab;
    @BindView(R.id.settings_fab)
    FloatingActionButton settingsFab;

    private Unbinder unbinder;
    private AppCompatActivity activity;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    private EpgSyncStatusReceiver epgSyncStatusReceiver;
    private boolean isServiceStarted;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.startup_fragment, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MainApplication.getComponent().inject(this);

        activity = (AppCompatActivity) getActivity();
        epgSyncStatusReceiver = new EpgSyncStatusReceiver(this);
        setHasOptionsMenu(true);

        String state;
        String details;
        if (savedInstanceState != null) {
            state = savedInstanceState.getString("state");
            details = savedInstanceState.getString("details");
            isServiceStarted = savedInstanceState.getBoolean("serviceStarted");
        } else {
            state = getString(R.string.initializing);
            details = "";
            isServiceStarted = false;
        }

        progressBar.setVisibility(View.INVISIBLE);

        if (appRepository.getConnectionData().getItems().size() == 0) {
            Timber.d("No connection available, showing settings");
            state = getString(R.string.no_connection_available);
            addConnectionFab.setVisibility(View.VISIBLE);
            addConnectionFab.setOnClickListener(v -> {
                showSettingsAddNewConnection();
            });

        } else if (appRepository.getConnectionData().getActiveItem() == null) {
            Timber.d("No active connection available, showing settings");
            state = getString(R.string.no_connection_active_advice);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                showConnectionListSettings();
            });

        } else if (appRepository.getChannelData().getItems().size() > 0) {
            Timber.d("Database not empty, showing main screen");
            // In case some channels are already available show the main screen. The service
            // will be started in the main activity when the network connection is available.
            // When the first initial sync is one a worker will be started to ping the server
            // in a certain interval to try to keep the connection alive.
            showContentScreen();

        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            Timber.d("Database is empty and no network is active to perform initial sync, showing settings");
            state = getString(R.string.err_no_network);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            });

        } else {
            Timber.d("Database is empty and network is active");
            LocalBroadcastManager.getInstance(activity).registerReceiver(epgSyncStatusReceiver, new IntentFilter(EpgSyncStatusReceiver.ACTION));
            // The database is empty and a full initial sync is required to get all data
            // for the first time. Start the service so it can connect to the server.
            // The current status is given via the broadcast receiver.
            if (!isServiceStarted) {
                Timber.d("Starting service for the first time");
                Intent intent = new Intent(activity, EpgSyncService.class);
                activity.stopService(intent);
                activity.startService(intent);
                isServiceStarted = true;
            } else {
                Timber.d("Service is already running");
            }
        }

        stateTextView.setText(state);
        detailsTextView.setText(details);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("state", stateTextView.getText().toString());
        outState.putString("details", detailsTextView.getText().toString());
        outState.putBoolean("serviceStarted", isServiceStarted);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(epgSyncStatusReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.startup_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showConnectionListSettings();
                return true;
            case R.id.menu_refresh:
                new MenuUtils(activity).handleMenuReconnectSelection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsAddNewConnection() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "add_connection");
        startActivity(intent);
    }

    private void showConnectionListSettings() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        startActivity(intent);
    }

    /**
     * Shows the main fragment like the channel list when the startup is complete.
     * Which fragment shall be shown is determined by a preference.
     */
    private void showContentScreen() {
        int startScreen = Integer.parseInt(sharedPreferences.getString("start_screen", "0"));
        Intent intent = new Intent(activity, NavigationActivity.class);
        intent.putExtra("startScreen", startScreen);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onEpgTaskStateChanged(EpgSyncTaskState state) {

        switch (state.getState()) {
            case DONE:
                Timber.d("Service sync state is done, starting main screen");
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                showContentScreen();
                break;

            case START:
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                break;

            case FAILED:
                Timber.d("Service sync state failed, showing settings");
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                progressBar.setVisibility(View.INVISIBLE);
                settingsFab.setVisibility(View.VISIBLE);
                settingsFab.setOnClickListener(v -> showConnectionListSettings());
                break;
        }
    }
}
