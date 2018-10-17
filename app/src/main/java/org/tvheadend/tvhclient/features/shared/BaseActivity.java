package org.tvheadend.tvhclient.features.shared;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailabilityChangedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.features.shared.receivers.NetworkStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;

import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity implements NetworkStatusReceiverCallback, NetworkStatusInterface, EpgSyncStatusCallback {

    private NetworkStatusReceiver networkStatusReceiver;
    private boolean isNetworkAvailable;
    private ServiceStatusReceiver serviceStatusReceiver;
    private SnackbarMessageReceiver snackbarMessageReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isNetworkAvailable = false;
        networkStatusReceiver = new NetworkStatusReceiver(this);
        serviceStatusReceiver = new ServiceStatusReceiver(this);
        snackbarMessageReceiver = new SnackbarMessageReceiver(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, new IntentFilter(SnackbarMessageReceiver.ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStatusReceiver, new IntentFilter(ServiceStatusReceiver.ACTION));
        registerReceiver(networkStatusReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStatusReceiver);
        unregisterReceiver(networkStatusReceiver);
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
            if (!isNetworkAvailable) {
                Timber.d("Network changed from offline to online, starting service");
                startService(new Intent(this, EpgSyncService.class));
            } else {
                Timber.d("Network still active, pinging server");
                Intent intent = new Intent(this, EpgSyncService.class);
                intent.setAction("getStatus");
                startService(intent);
            }
        } else {
            Timber.d("Network is not available anymore, stopping service");
            stopService(new Intent(this, EpgSyncService.class));
        }
        isNetworkAvailable = isAvailable;

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment instanceof NetworkAvailabilityChangedInterface) {
            ((NetworkAvailabilityChangedInterface) fragment).onNetworkAvailabilityChanged(isAvailable);
        }

        fragment = getSupportFragmentManager().findFragmentById(R.id.details);
        if (fragment instanceof NetworkAvailabilityChangedInterface) {
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
        Timber.d("Epg task state changed to " + state.getState() + ", message is " + state.getMessage());
        switch (state.getState()) {
            case CLOSED:
                if (getCurrentFocus() != null) {
                    Snackbar.make(getCurrentFocus(), state.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
                stopService(new Intent(this, EpgSyncService.class));
                startService(new Intent(this, EpgSyncService.class));
                break;

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
    public void onBackPressed() {

        int count = getSupportFragmentManager().getBackStackEntryCount();
        boolean navHistoryEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("navigation_history_enabled", true);

        if (navHistoryEnabled && count <= 1) {
            finish();
        } else if (!navHistoryEnabled && count < 1) {
            // Only exit the app when there is no more fragments in the stack.
            // When the program list is visible a back press shall show the channel list again
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
