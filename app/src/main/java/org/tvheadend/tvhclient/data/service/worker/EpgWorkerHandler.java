package org.tvheadend.tvhclient.data.service.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class EpgWorkerHandler {

    public static final String WORKER_TAG = "tvhclient_worker";

    public static void startBackgroundWorkers(Context context) {
        Timber.d("Starting background workers");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Get the time that defines how many hours of epg data the app shall load when a sync is done
        // This will be used to determine how often the background service shall be started
        // The default is 90% of the defined time to always have some data available
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long epgMaxTime = Long.parseLong(sharedPreferences.getString("epg_max_time", context.getResources().getString(R.string.pref_default_epg_max_time)));
        long time = epgMaxTime - (epgMaxTime / 10);

        Timber.d("Epg data update worker interval is " + time + " seconds");
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
}
