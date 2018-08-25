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
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.MainActivity;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.utils.MenuUtils;
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
    private ServiceStatusReceiver serviceStatusReceiver;
    private boolean isServiceStarted;
    private String stateText;
    private String detailsText;

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
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            stateText = savedInstanceState.getString("stateText");
            detailsText = savedInstanceState.getString("detailsText");
            isServiceStarted = savedInstanceState.getBoolean("isServiceStarted");
        } else {
            stateText = getString(R.string.initializing);
            detailsText = "";
            isServiceStarted = false;
        }
        handleStartupProcedure();
    }

    private void handleStartupProcedure() {
        Timber.d("start");

        if (appRepository.getConnectionData().getItems().size() == 0) {
            Timber.d("No connection available, showing settings");
            stateText = getString(R.string.no_connection_available);
            progressBar.setVisibility(View.INVISIBLE);
            addConnectionFab.setVisibility(View.VISIBLE);
            addConnectionFab.setOnClickListener(v -> {
                showSettingsAddNewConnection();
            });

        } else if (appRepository.getConnectionData().getActiveItem() == null) {
            Timber.d("No active connection available, showing settings");
            stateText = getString(R.string.no_connection_active_advice);
            progressBar.setVisibility(View.INVISIBLE);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                showConnectionListSettings();
            });

        } else if (NetworkUtils.isNetworkAvailable(activity)
                && appRepository.getChannelData().getItems().size() > 0) {
            Timber.d("Network is available and database contains channels, starting service and showing contents");
            showContentScreen();

        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            Timber.d("No network is active to perform initial sync, showing settings");
            stateText = getString(R.string.err_no_network);
            progressBar.setVisibility(View.INVISIBLE);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            });

        } else {
            Timber.d("Database is empty and network is active, starting service to perform initial sync");
            // Create and register the broadcast receiver to get the sync status
            serviceStatusReceiver = new ServiceStatusReceiver(this);
            LocalBroadcastManager.getInstance(activity).registerReceiver(serviceStatusReceiver, new IntentFilter(ServiceStatusReceiver.ACTION));

            // The database is empty and a full initial sync is required.
            // Start the service so it can connect to the server.
            // The sync status will be received via the broadcast receiver.
            if (!isServiceStarted) {
                Timber.d("Starting service for the first time");
                activity.startService(new Intent(activity, EpgSyncService.class));
                isServiceStarted = true;
            } else {
                Timber.d("Service was already started");
            }
        }

        stateTextView.setText(stateText);
        detailsTextView.setText(detailsText);
        Timber.d("end");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("stateText", stateTextView.getText().toString());
        outState.putString("detailsText", detailsTextView.getText().toString());
        outState.putBoolean("isServiceStarted", isServiceStarted);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(serviceStatusReceiver);
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
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("startScreen", startScreen);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onEpgTaskStateChanged(EpgSyncTaskState state) {
        switch (state.getState()) {
            case CONNECTING:
            case CONNECTED:
            case SYNC_STARTED:
                Timber.d("Service sync is starting or loading data");
                progressBar.setVisibility(View.VISIBLE);
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                break;

            case SYNC_DONE:
                Timber.d("Service sync is done, starting main screen");
                progressBar.setVisibility(View.INVISIBLE);
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                showContentScreen();
                break;

            case FAILED:
                Timber.d("Service sync failed, showing settings");
                stateTextView.setText(state.getMessage());
                detailsTextView.setText(state.getDetails());
                progressBar.setVisibility(View.INVISIBLE);
                settingsFab.setVisibility(View.VISIBLE);
                settingsFab.setOnClickListener(v -> showConnectionListSettings());
                break;
        }
    }
}
