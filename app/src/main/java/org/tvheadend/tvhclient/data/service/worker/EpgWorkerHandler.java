package org.tvheadend.tvhclient.data.service.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class EpgWorkerHandler implements HtspConnection.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String WORKER_TAG = "tvhclient_worker";
    private final Context context;

    public EpgWorkerHandler(Context context) {
        this.context = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void setConnection(@NonNull HtspConnection connection) {

    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        Timber.d("Connection state changed to " + state);
        switch (state) {
            case CONNECTED:
                startBackgroundWorkers();
                break;
        }
    }

    private void startBackgroundWorkers() {
        Timber.d("Starting background workers");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Get the time that defines how many hours of epg data the app shall load when a sync is done
        // This will be used to determine how often the background service shall be started
        // The default is 90% of the defined time to always have some data available
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultEpgMaxTime = context.getResources().getString(R.string.pref_default_epg_max_time);
        long epgMaxTime = Long.parseLong(sharedPreferences.getString("epg_max_time", defaultEpgMaxTime));
        long time = epgMaxTime - (epgMaxTime / 10);

        Timber.d("Epg data update worker interval is " + time + " minutes");
        PeriodicWorkRequest updateWorkRequest =
                new PeriodicWorkRequest.Builder(EpgDataUpdateWorker.class, time, TimeUnit.SECONDS)
                        .setConstraints(constraints)
                        .addTag(WORKER_TAG)
                        .build();

        Timber.d("Epg data removal worker interval is 1 day");
        PeriodicWorkRequest removalWorkRequest =
                new PeriodicWorkRequest.Builder(EpgDataRemovalWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .addTag(WORKER_TAG)
                        .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork("update_epg", ExistingPeriodicWorkPolicy.KEEP, updateWorkRequest);
        WorkManager.getInstance().enqueueUniquePeriodicWork("remove_outdated_epg", ExistingPeriodicWorkPolicy.KEEP, removalWorkRequest);

        Timber.d("Started background workers");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "epg_max_time":
                Timber.d("Preference " + key + " has changed, restarting background workers");
                WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG);
                startBackgroundWorkers();
                break;
        }
    }
}
