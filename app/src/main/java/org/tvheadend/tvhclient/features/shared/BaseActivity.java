package org.tvheadend.tvhclient.features.shared;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingDetailsFragment;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingDetailsFragment;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingDetailsFragment;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsFragment;
import org.tvheadend.tvhclient.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailabilityChangedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.features.shared.receivers.NetworkStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;

import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity implements NetworkStatusReceiverCallback, NetworkStatusInterface {

    private NetworkStatusReceiver networkStatusReceiver;
    private boolean isNetworkAvailable;
    private SnackbarMessageReceiver snackbarMessageReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkStatusReceiver = new NetworkStatusReceiver(this);
        snackbarMessageReceiver = new SnackbarMessageReceiver(this);

        if (savedInstanceState == null) {
            isNetworkAvailable = false;
        } else {
            isNetworkAvailable = savedInstanceState.getBoolean("isNetworkAvailable", false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isNetworkAvailable", isNetworkAvailable);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, new IntentFilter(SnackbarMessageReceiver.ACTION));
        registerReceiver(networkStatusReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver);
        unregisterReceiver(networkStatusReceiver);
    }

    @Override
    public void onNetworkStatusChanged(boolean isNetworkAvailable) {
        onNetworkAvailabilityChanged(isNetworkAvailable);
        if (!isNetworkAvailable && getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), "No network available.", Snackbar.LENGTH_SHORT).show();
        }
    }

    protected void onNetworkAvailabilityChanged(boolean isAvailable) {
        if (isAvailable) {
            if (!isNetworkAvailable) {
                Timber.d("Network changed from offline to online, starting service");
                if (MainApplication.isActivityVisible()) {
                    startService(new Intent(this, EpgSyncService.class));
                }
            } else {
                Timber.d("Network still active, pinging server");
                Intent intent = new Intent(this, EpgSyncService.class);
                intent.setAction("getStatus");
                if (MainApplication.isActivityVisible()) {
                    startService(intent);
                }
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
    public void onBackPressed() {
        boolean navigationHistoryEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("navigation_history_enabled", true);
        if (!navigationHistoryEnabled) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
            if (fragment instanceof ProgramListFragment
                    || fragment instanceof ProgramDetailsFragment
                    || fragment instanceof RecordingDetailsFragment
                    || fragment instanceof SeriesRecordingDetailsFragment
                    || fragment instanceof TimerRecordingDetailsFragment) {
                // Do not finish the activity in case the program list fragment is visible which
                // was called from the channel list fragment. In this case go back to the
                // channel list fragment. Then a new back press can finish the activity.
                super.onBackPressed();
            } else {
                finish();
            }
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }
}
