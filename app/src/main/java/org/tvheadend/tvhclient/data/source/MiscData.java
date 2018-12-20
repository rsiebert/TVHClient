package org.tvheadend.tvhclient.data.source;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.features.settings.DatabaseClearedCallback;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class MiscData extends BaseData {

    private final AppRoomDatabase db;
    private static WeakReference<DatabaseClearedCallback> callback;

    @Inject
    public MiscData(AppRoomDatabase database) {
        this.db = database;
    }

    public void clearDatabase(Context context, DatabaseClearedCallback callback) {
        MiscData.callback = new WeakReference<>(callback);
        new ClearDatabaseTask(context, db).execute();
    }

    private static class ClearDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final WeakReference<Context> context;

        ClearDatabaseTask(Context context, AppRoomDatabase db) {
            this.context = new WeakReference<>(context);
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.getChannelDao().deleteAll();
            db.getChannelTagDao().deleteAll();
            db.getTagAndChannelDao().deleteAll();
            db.getProgramDao().deleteAll();
            db.getRecordingDao().deleteAll();
            db.getSeriesRecordingDao().deleteAll();
            db.getTimerRecordingDao().deleteAll();
            db.getServerProfileDao().deleteAll();

            // Clear all assigned profiles
            for (Connection connection : db.getConnectionDao().loadAllConnectionsSync()) {
                ServerStatus serverStatus = db.getServerStatusDao().loadServerStatusByIdSync(connection.getId());
                if (serverStatus != null) {
                    serverStatus.setHtspPlaybackServerProfileId(0);
                    serverStatus.setHttpPlaybackServerProfileId(0);
                    serverStatus.setCastingServerProfileId(0);
                    serverStatus.setRecordingServerProfileId(0);
                    db.getServerStatusDao().update(serverStatus);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = this.context.get();
            if (context == null) {
                return;
            }
            Intent intent = new Intent(SnackbarMessageReceiver.ACTION);
            intent.putExtra(SnackbarMessageReceiver.CONTENT, "Database cleared");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            DatabaseClearedCallback databaseClearedCallback = callback.get();
            if (databaseClearedCallback != null) {
                databaseClearedCallback.onDatabaseCleared();
            }
        }
    }
}
