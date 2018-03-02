package org.tvheadend.tvhclient.ui.startup;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.service.EpgSyncService;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.navigation.NavigationActivity;
import org.tvheadend.tvhclient.ui.settings.SettingsActivity;
import org.tvheadend.tvhclient.ui.settings.SettingsManageConnectionActivity;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class StartupFragment extends Fragment {
    @SuppressWarnings("unused")
    private String TAG = getClass().getSimpleName();

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status_text)
    TextView statusTextView;
    @BindView(R.id.status_background)
    ImageView statusImageView;
    @BindView(R.id.fab)
    FloatingActionButton floatingActionButton;

    private String status;
    private String title;
    private Unbinder unbinder;
    private AppCompatActivity activity;
    private ConnectionRepository repository;
    private ToolbarInterface toolbarInterface;
    private SharedPreferences sharedPreferences;

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
            toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle("Connection Status");
        }
        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        repository = new ConnectionRepository(activity);

        if (!isConnectionDefined()) {
            statusTextView.setText("No connection defined...");
            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setOnClickListener(v -> {
                showSettingsAddNewConnection();
            });
        } else if (!isActiveConnectionDefined()) {
            statusTextView.setText("At least one connection is defined but not active...");
            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setOnClickListener(v -> {
                showSettingsListConnections();
            });
        } else if (!isNetworkAvailable()) {
            statusTextView.setText("No network available, please activate wifi or mobile data...");
            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            });
        } else {
            // Restore the last shown state when an orientation change happened.
            // Otherwise start the service when the fragment was created first
            if (savedInstanceState != null) {
                status = savedInstanceState.getString("status");
                title = savedInstanceState.getString("title");
                statusTextView.setText(status);
                toolbarInterface.setTitle(title);
            } else {
                Intent intent = new Intent(activity, EpgSyncService.class);
                activity.stopService(intent);
                activity.startService(intent);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("status", status);
        outState.putString("title", title);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("service_status");
        LocalBroadcastManager.getInstance(activity).registerReceiver(messageReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
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
                showSettingsListConnections();
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
        Log.d(TAG, "isConnectionDefined: ");
        List<Connection> connectionList = repository.getAllConnectionsSync();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionDefined() {
        Log.d(TAG, "isActiveConnectionDefined: ");
        return repository.getActiveConnectionSync() != null;
    }

    private void showSettingsAddNewConnection() {
        Intent intent = new Intent(activity, SettingsManageConnectionActivity.class);
        startActivity(intent);
    }

    private void showSettingsListConnections() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        startActivity(intent);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the connection status from the local broadcast
            if (intent.hasExtra("connection_status")) {
                title = "Connection Status";
                status = (String) intent.getSerializableExtra("connection_status");
            }
            // Get the current authentication status from the local broadcast
            if (intent.hasExtra("authentication_status")) {
                status = (String) intent.getSerializableExtra("authentication_status");
            }
            // Inform the fragment about the current sync status
            if (intent.hasExtra("sync_status")) {
                title = "Sync Status";
                status = intent.getStringExtra("sync_status");
                // When this is received the connection is successfully established with
                // the server and any possible sync has been finished
                if (intent.getStringExtra("sync_status").equals("done")) {
                    showContentScreen();
                    startBackgroundServices();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
            statusTextView.setText(status);
            toolbarInterface.setTitle(title);
        }
    };

    private void startBackgroundServices() {
        Log.d(TAG, "startBackgroundServices() called");

        Intent epgFetchIntent = new Intent(activity, EpgSyncService.class);
        epgFetchIntent.setAction("getMoreEvents");
        PendingIntent epgFetchPendingIntent = PendingIntent.getService(activity, 0, epgFetchIntent, 0);

        Intent epgRemovalIntent = new Intent(activity, EpgSyncService.class);
        epgRemovalIntent.setAction("deleteEvents");
        PendingIntent epgRemovalPendingIntent = PendingIntent.getService(activity, 0, epgRemovalIntent, 0);

        // The time when the service shall be called periodically
        int epgFetchIntervalTime = Integer.valueOf(sharedPreferences.getString("pref_epg_fetch_interval", "15")) * 60 * 1000;
        int epgRemovalIntervalTime = Integer.valueOf(sharedPreferences.getString("pref_epg_removal_interval", "120")) * 60 * 1000;

        // Start the alarms one minute after the service has been started for the first time
        long firstTriggerTime = Calendar.getInstance().getTimeInMillis() * 60 * 1000;
        // Use the alarm manager to start the service every x minutes
        AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstTriggerTime, epgFetchIntervalTime, epgFetchPendingIntent);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstTriggerTime, epgRemovalIntervalTime, epgRemovalPendingIntent);
        }
    }

    private void stopService() {
        // Unregister from the broadcast manager to avoid getting any connection
        // state changes when the service is stopped and the connection gets closed.
        // The user needs to go to the settings and fix the login credentials.
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(messageReceiver);
        activity.stopService(new Intent(activity, EpgSyncService.class));
    }

    private void showContentScreen() {
        // Get the initial screen from the user preference.
        // This determines which screen shall be shown first
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int startScreen = Integer.parseInt(sharedPreferences.getString("defaultMenuPositionPref", "0"));
        Intent intent = new Intent(activity, NavigationActivity.class);
        intent.putExtra("navigation_menu_position", startScreen);
        activity.startActivity(intent);
        activity.finish();
    }
}
