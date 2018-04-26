package org.tvheadend.tvhclient.features.startup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.remote.EpgSyncService;
import org.tvheadend.tvhclient.data.remote.EpgSyncTask;
import org.tvheadend.tvhclient.data.remote.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.remote.htsp.tasks.Authenticator;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.features.navigation.NavigationActivity;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.features.settings.SettingsAddEditConnectionActivity;
import org.tvheadend.tvhclient.features.shared.MenuUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO add nice background image
// TODO use translated strings

public class StartupFragment extends Fragment {

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
    private ConnectionRepository repository;
    private SharedPreferences sharedPreferences;
    private String state;
    private String details;

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

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.startup));
        }
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        repository = new ConnectionRepository(activity);

        progressBar.setVisibility(View.INVISIBLE);

        if (!isConnectionDefined()) {
            state = getString(R.string.no_connection_available);
            addConnectionFab.setVisibility(View.VISIBLE);
            addConnectionFab.setOnClickListener(v -> {
                showSettingsAddNewConnection();
            });
        } else if (!isActiveConnectionDefined()) {
            state = getString(R.string.no_connection_active_advice);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                showConnectionListSettings();
            });
        } else if (!isNetworkAvailable()) {
            state = getString(R.string.err_no_network);
            settingsFab.setVisibility(View.VISIBLE);
            settingsFab.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            });
        } else {
            progressBar.setVisibility(View.VISIBLE);

            if (savedInstanceState != null) {
                state = savedInstanceState.getString("state");
                details = savedInstanceState.getString("details");
            } else {
                state = getString(R.string.initializing);
                details = "";
                // Call the service to get the connectivity status. If the service is not
                // connected to the server anymore it will open a connection and start an
                // initial sync, otherwise the sync is skipped and the main screen can be shown.
                Intent intent = new Intent(activity, EpgSyncService.class);
                intent.setAction("getStatus");
                activity.startService(intent);
            }
        }

        stateTextView.setText(state);
        detailsTextView.setText(details);
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
        // Register the BroadcastReceiver so that the connection
        // and sync messages from the service can be received and shown
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("service_status");
        LocalBroadcastManager.getInstance(activity).registerReceiver(messageReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister the BroadcastReceiver when this fragment is not active anymore
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(messageReceiver);
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private boolean isConnectionDefined() {
        List<Connection> connectionList = repository.getAllConnectionsSync();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionDefined() {
        return repository.getActiveConnectionSync() != null;
    }

    private void showSettingsAddNewConnection() {
        Intent intent = new Intent(activity, SettingsAddEditConnectionActivity.class);
        startActivity(intent);
    }

    private void showConnectionListSettings() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        startActivity(intent);
    }

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "state" extra. Any details about the state is given
     * via the "details" extra. When the extra "done" was received the startup of the app
     * is considered done. The pending intents for the background data sync are started.
     * Finally the defined main fragment like the channel list will be shown.
     */
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean showSettingsButton = false;

            if (intent.hasExtra("connection_state")) {
                HtspConnection.State state = (HtspConnection.State) intent.getSerializableExtra("connection_state");
                if (state == HtspConnection.State.CLOSED) {
                    stateTextView.setText("Connection closed");
                } else if (state == HtspConnection.State.CONNECTED) {
                    stateTextView.setText("Connected");
                    detailsTextView.setText("");
                } else if (state == HtspConnection.State.CONNECTING) {
                    stateTextView.setText("Connecting...");
                } else if (state == HtspConnection.State.FAILED) {
                    stateTextView.setText("Connection failed");
                    showSettingsButton = true;
                } else if (state == HtspConnection.State.FAILED_UNRESOLVED_ADDRESS) {
                    detailsTextView.setText("Failed to resolve server address");
                    showSettingsButton = true;
                } else if (state == HtspConnection.State.FAILED_EXCEPTION_OPENING_SOCKET) {
                    detailsTextView.setText("Error while opening a connection to the server");
                    showSettingsButton = true;
                } else if (state == HtspConnection.State.FAILED_CONNECTING_TO_SERVER) {
                    detailsTextView.setText("Failed to connect to server");
                    showSettingsButton = true;
                }

            } else if (intent.hasExtra("authentication_state")) {
                Authenticator.State state = (Authenticator.State) intent.getSerializableExtra("authentication_state");
                detailsTextView.setText("");
                if (state == Authenticator.State.AUTHENTICATING) {
                    stateTextView.setText("Authenticating...");
                } else if (state == Authenticator.State.AUTHENTICATED) {
                    stateTextView.setText("Authenticated");
                } else if (state == Authenticator.State.FAILED_BAD_CREDENTIALS) {
                    stateTextView.setText("Authentication failed, bad username or password");
                    showSettingsButton = true;
                }

            } else if (intent.hasExtra("sync_state")) {
                EpgSyncTask.State state = (EpgSyncTask.State) intent.getSerializableExtra("sync_state");
                if (state == EpgSyncTask.State.LOADING) {
                    stateTextView.setText("Receiving available data from server");
                    if (intent.hasExtra("sync_details")) {
                        String details = (String) intent.getSerializableExtra("sync_details");
                        detailsTextView.setText(details);
                    }

                } else if (state == EpgSyncTask.State.SAVING) {
                    stateTextView.setText("Saving available data into database");
                    detailsTextView.setText("");

                } else if (state == EpgSyncTask.State.DONE) {
                    progressBar.setVisibility(View.INVISIBLE);
                    stateTextView.setText("Done");
                    detailsTextView.setText("");

                    showContentScreen();
                    startBackgroundServices();
                    activity.finish();

                } else if (state == EpgSyncTask.State.RECONNECT) {
                    progressBar.setVisibility(View.INVISIBLE);
                    stateTextView.setText("Reconnecting to server");
                    detailsTextView.setText("");

                    activity.stopService(new Intent(activity, EpgSyncService.class));
                    activity.startService(new Intent(activity, EpgSyncService.class));
                }
            }

            progressBar.setVisibility(showSettingsButton ? View.INVISIBLE : View.VISIBLE);
            settingsFab.setVisibility(showSettingsButton ? View.VISIBLE : View.INVISIBLE);
            settingsFab.setOnClickListener(v -> showConnectionListSettings());
        }
    };

    /**
     * Defines the two intents that will periodically get more events and remove outdated
     * events from the database in the background. The alarm manager is used to start the
     * intents after the specified time intervals. The time interval is read from preferences.
     */
    private void startBackgroundServices() {

        Intent epgFetchIntent = new Intent(activity, EpgSyncService.class);
        epgFetchIntent.setAction("getMoreEvents");
        PendingIntent epgFetchPendingIntent = PendingIntent.getService(activity, 0, epgFetchIntent, 0);

        Intent epgRemovalIntent = new Intent(activity, EpgSyncService.class);
        epgRemovalIntent.setAction("deleteEvents");
        PendingIntent epgRemovalPendingIntent = PendingIntent.getService(activity, 0, epgRemovalIntent, 0);

        int epgFetchIntervalTime = Integer.valueOf(sharedPreferences.getString("epg_fetch_time_interval", "2")) * 1000 * 60 * 60;
        int epgRemovalIntervalTime = Integer.valueOf(sharedPreferences.getString("epg_removal_time_interval", "4")) * 1000 * 60 * 60;

        // Start the intents two minutes after the service has been started for the first time
        long firstTriggerTime = new Date().getTime() + (1000 * 60 * 2);
        // Use the alarm manager to start the intents after the specified intervals
        AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstTriggerTime, epgFetchIntervalTime, epgFetchPendingIntent);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstTriggerTime, epgRemovalIntervalTime, epgRemovalPendingIntent);
        }
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
    }
}
