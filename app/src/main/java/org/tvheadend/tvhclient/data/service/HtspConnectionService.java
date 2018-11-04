package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class HtspConnectionService extends Service {

    @Inject
    protected AppRepository appRepository;
    private SimpleHtspConnection simpleHtspConnection;
    private Connection connection;
    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public HtspConnectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return HtspConnectionService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public SimpleHtspConnection getSimpleHtspConnection() {
        return simpleHtspConnection;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Starting service");

        MainApplication.getComponent().inject(this);
        connection = appRepository.getConnectionData().getActiveItem();
        simpleHtspConnection = new SimpleHtspConnection(connection);
        simpleHtspConnection.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Received command for service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Stopping service");
        if (simpleHtspConnection != null) {
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
        connection = null;
    }
}
