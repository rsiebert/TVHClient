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
        Timber.d("Enqueuing work " + work.getAction());
        enqueueWork(context, EpgSyncIntentService.class, 1, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Timber.d("handling work " + intent.getAction());
        if (epgSyncHandler.isConnected()) {
            Timber.d("Connected to server, passing intent " + intent.getAction() + " to epg handler");
            epgSyncHandler.handleIntent(intent);
        }
    }
}
