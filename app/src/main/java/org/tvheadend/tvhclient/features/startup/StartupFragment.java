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
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusReceiver;
import org.tvheadend.tvhclient.features.navigation.NavigationActivity;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.features.shared.MenuUtils;
import org.tvheadend.tvhclient.utils.NetworkUtils;

import java.util.List;

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
    private String state;
    private String details;
    private MenuUtils menuUtils;
    private EpgSyncStatusReceiver epgSyncStatusReceiver;

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
        Timber.d("start");

        MainApplication.getComponent().inject(this);

        activity = (AppCompatActivity) getActivity();
        menuUtils = new MenuUtils(activity);
        epgSyncStatusReceiver = new EpgSyncStatusReceiver(this);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            state = savedInstanceState.getString("state");
            details = savedInstanceState.getString("details");
        } else {
            state = getString(R.string.initializing);
            details = "";
        }

        progressBar.setVisibility(View.INVISIBLE);

        if (!isConnectionAvailable()) {
            Timber.d("No connection available, showing settings");
            state = getString(R.string.no_connection_available);
            addConnectionFab.setVisibility(View.VISIBLE);
            addConnectionFab.setOnClickListener(v -> {
                showSettingsAddNewConnection();
            });

        } else if (!isActiveConnectionAvailable()) {
            Timber.d("No active connection available, showing settings");
            state = getString(R.string.no_connection_active_advice);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                showConnectionListSettings();
            });

        } else if (appRepository.getChannelData().getItems() != null
                && appRepository.getChannelData().getItems().size() > 0) {
            Timber.d("Database contains channels, showing main screen");
            // Do not start the service and go to the main screen to speed things up.
            // The base activity listens for network changes via the NetworkStatusReceiver class.
            // It informs the base activity if the network is available or not. The base activity
            // will the either start or stop the service via the EpgStatusHandler class.

            //activity.startService(new Intent(activity, EpgSyncService.class).setAction("getStatus"));
            Timber.d("Starting worker to start service");
            //OneTimeWorkRequest x = new OneTimeWorkRequest.Builder(EpgServiceStartupWorker.class)
                    //.setInitialDelay(5, TimeUnit.SECONDS)
            //        .build();
            //WorkManager.getInstance().enqueue(x);

            Timber.d("Showing main screen");
            showContentScreen();

        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            Timber.d("No network is active to perform initial sync, showing settings");
            state = getString(R.string.err_no_network);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            });

        } else {
            Timber.d("Network is active, starting service");
            progressBar.setVisibility(View.VISIBLE);
            // Start the service it will send its status via the broadcast receiver.
            // Depending if the service is already connected or not the receiver will
            // either start the initial sync or reconnect to the server. We will be
            // notified of the sync being done via the onEpgSyncStateChanged method.
            // Then we show the main screen.
            activity.startService(new Intent(activity, EpgSyncService.class).setAction("getStatus"));
        }

        stateTextView.setText(state);
        detailsTextView.setText(details);

        Timber.d("end");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("state", state);
        outState.putString("details", details);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(activity).registerReceiver(epgSyncStatusReceiver, new IntentFilter(EpgSyncStatusReceiver.ACTION));
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
                menuUtils.handleMenuReconnectSelection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isConnectionAvailable() {
        List<Connection> connectionList = appRepository.getConnectionData().getItems();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionAvailable() {
        return appRepository.getConnectionData().getActiveItem() != null;
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
     * Which fragment shall be shown is determined from the user preference.
     */
    private void showContentScreen() {
        int startScreen = Integer.parseInt(sharedPreferences.getString("start_screen", "0"));
        Intent intent = new Intent(activity, NavigationActivity.class);
        intent.putExtra("startScreen", startScreen);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onEpgSyncMessageChanged(String msg, String details) {
        stateTextView.setText(msg);
        detailsTextView.setText(details);
    }

    @Override
    public void onEpgSyncStateChanged(EpgSyncStatusReceiver.State state) {
        if (state == EpgSyncStatusReceiver.State.DONE) {
            Timber.d("Service sync state is done, starting main screen");
            showContentScreen();
        } else {
            Timber.d("Service sync state failed, showing settings");
            progressBar.setVisibility(View.INVISIBLE);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> showConnectionListSettings());
        }
    }
}
