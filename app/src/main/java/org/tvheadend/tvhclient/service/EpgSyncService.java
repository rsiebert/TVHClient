package org.tvheadend.tvhclient.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.service.htsp.SimpleHtspConnection;

// TODO add removing data older that x days
// TODO add task to periodically fetch epg data for all channels

public class EpgSyncService extends Service {
    private static final String TAG = EpgSyncService.class.getSimpleName();

    private HandlerThread handlerThread;
    private Connection connection;
    private SimpleHtspConnection simpleHtspConnection;
    private EpgSyncTask epgSyncTask;
    private SharedPreferences sharedPreferences;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding not allowed");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting EPG Sync Service");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        handlerThread = new HandlerThread("EpgSyncService Handler Thread");
        handlerThread.start();
        new Handler(handlerThread.getLooper());

        connection = new ConnectionRepository(this).getActiveConnectionSync();
        if (connection == null) {
            Log.i(TAG, "No account configured, aborting startup of EPG Sync Service");
            stopSelf();
            return;
        }

        openConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (connection == null) {
            return START_NOT_STICKY;
        }
        // Forward intents that contain an action to the epg sync task.
        // This task will then execute the desired actions
        if (intent != null && intent.getAction() != null) {
            epgSyncTask.getHandler().post(() -> epgSyncTask.handleIntent(intent));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stopping EPG Sync Service");
        closeConnection();
        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread.interrupt();
            handlerThread = null;
        }
    }

    /**
     * Before a connection to a server is made to get all initial data, compare the id of the
     * currently active connection with the one that was used before. If they differ
     * clear the database to prevent having data that does not belong to the currently
     * used connection.
     */
    private void maybeClearDatabaseTablesDueToNewConnection() {
        // Get the id of the previously used connection
        long id = sharedPreferences.getLong("previous_connection_id", -1);
        if (id != connection.getId()) {

            AppDatabase appDatabase = AppDatabase.getInstance(this.getApplicationContext());
            new ClearTablesAsyncTask(appDatabase).execute();
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            // Save the id of the new connection in the preferences.
            editor.putLong("previous_connection_id", connection.getId());
            // Save the status of the initial sync in the preferences.
            editor.putBoolean("initial_sync_done", false);
            editor.apply();
        }
    }

    private static class ClearTablesAsyncTask extends AsyncTask<Void, Void, Void> {
        private AppDatabase db;

        ClearTablesAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.channelDao().deleteAll();
            db.channelTagDao().deleteAll();
            db.tagAndChannelDao().deleteAll();
            db.programDao().deleteAll();
            db.recordingDao().deleteAll();
            db.seriesRecordingDao().deleteAll();
            db.timerRecordingDao().deleteAll();
            return null;
        }
    }

    private void openConnection() {
        Log.d(TAG, "openConnection() called");

        maybeClearDatabaseTablesDueToNewConnection();

        simpleHtspConnection = new SimpleHtspConnection(getApplicationContext(), connection);
        epgSyncTask = new EpgSyncTask(this, simpleHtspConnection);
        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    private void closeConnection() {
        if (epgSyncTask != null) {
            simpleHtspConnection.removeMessageListener(epgSyncTask);
            simpleHtspConnection.removeAuthenticationListener(epgSyncTask);
            epgSyncTask = null;
        }
        if (simpleHtspConnection != null) {
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
    }
}
