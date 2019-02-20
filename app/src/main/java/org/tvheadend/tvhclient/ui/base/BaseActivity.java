package org.tvheadend.tvhclient.ui.base;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingDetailsFragment;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingDetailsFragment;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingDetailsFragment;
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsFragment;
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.NetworkStatusListener;
import org.tvheadend.tvhclient.util.network.NetworkStatusReceiver;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarMessageReceiver;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity implements NetworkStatusListener {

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
        if (!isNetworkAvailable) {
            SnackbarUtils.sendSnackbarMessage(this, "No network available");
        }
    }

    protected void onNetworkAvailabilityChanged(boolean isAvailable) {
        if (isAvailable) {
            if (!isNetworkAvailable) {
                Timber.d("Network changed from offline to online, starting service");
                if (MainApplication.isActivityVisible()) {
                    Intent intent = new Intent(this, HtspService.class);
                    intent.setAction("connect");
                    startService(intent);
                }
            } else {
                Timber.d("Network still active, pinging server");
                if (MainApplication.isActivityVisible()) {
                    Intent intent = new Intent(this, HtspService.class);
                    intent.setAction("reconnect");
                    startService(intent);
                }
            }
        } else {
            Timber.d("Network is not available anymore, stopping service");
            stopService(new Intent(this, HtspService.class));
        }
        isNetworkAvailable = isAvailable;

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment instanceof NetworkStatusListener) {
            ((NetworkStatusListener) fragment).onNetworkStatusChanged(isAvailable);
        }

        fragment = getSupportFragmentManager().findFragmentById(R.id.details);
        if (fragment instanceof NetworkStatusListener) {
            ((NetworkStatusListener) fragment).onNetworkStatusChanged(isAvailable);
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
