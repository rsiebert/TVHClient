package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import org.tvheadend.tvhclient.MainApplication;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgSyncIntentService extends JobIntentService {

    @Inject
    protected EpgSyncHandler epgSyncHandler;

    public EpgSyncIntentService() {
        MainApplication.getComponent().inject(this);
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, EpgSyncIntentService.class, 1, work);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("All work complete");
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (!epgSyncHandler.isConnected()) {
            Timber.d("Not connected to server, initializing and connecting to server");
            epgSyncHandler.stop();
            if (!epgSyncHandler.init()) {
                Timber.i("No connection available, not starting service");
                stopSelf();
            }
            epgSyncHandler.connect();
        } else {
            Timber.d("Connected to server, passing intent to epg sync task");
            epgSyncHandler.handleIntent(intent);
        }
    }
}