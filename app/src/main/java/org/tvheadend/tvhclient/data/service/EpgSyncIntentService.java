package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import timber.log.Timber;

public class EpgSyncIntentService extends JobIntentService implements Authenticator.Listener {

    @Inject
    protected AppRepository appRepository;
    private SimpleHtspConnection simpleHtspConnection;
    private Connection connection;
    private EpgSyncTask epgSyncTask;

    public EpgSyncIntentService() {
        Timber.d("Starting service");
        MainApplication.getComponent().inject(this);
        connection = appRepository.getConnectionData().getActiveItem();
        simpleHtspConnection = new SimpleHtspConnection(connection);
        simpleHtspConnection.start();
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, EpgSyncIntentService.class, 1, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Timber.d("Handling work for service");

        if (simpleHtspConnection != null
                && simpleHtspConnection.isConnected()
                && simpleHtspConnection.isAuthenticated()) {
            Timber.d("Connected to server, passing work to epg sync task");
            epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);
            epgSyncTask.handleIntent(intent);
        } else {
            Timber.d("Not connected to server");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Stopping service");
        if (simpleHtspConnection != null) {
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
        epgSyncTask = null;
        connection = null;
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {

    }
}