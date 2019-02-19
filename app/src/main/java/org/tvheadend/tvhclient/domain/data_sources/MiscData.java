package org.tvheadend.tvhclient.domain.data_sources;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.ui.features.settings.DatabaseClearedCallback;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class MiscData {

    private final AppRoomDatabase db;
    private static WeakReference<DatabaseClearedCallback> callback;

    public MiscData(AppRoomDatabase database) {
        this.db = database;
    }

    public void clearDatabase(Context context, DatabaseClearedCallback callback) {
        MiscData.callback = new WeakReference<>(callback);
        new ClearDatabaseTask(context, db).execute();
    }

    private static class ClearDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final ProgressDialog dialog;
        private final String msg;

        ClearDatabaseTask(Context context, AppRoomDatabase db) {
            this.db = db;
            this.dialog = new ProgressDialog(context);
            this.msg = context.getString(R.string.deleting_database_contents);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage(msg);
            dialog.setIndeterminate(true);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Timber.d("Deleting database contents...");

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
                connection.setLastUpdate(0);
                connection.setSyncRequired(true);
                db.getConnectionDao().update(connection);

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
            Timber.d("Deleting database contents finished");
            dialog.dismiss();
            DatabaseClearedCallback databaseClearedCallback = callback.get();
            if (databaseClearedCallback != null) {
                databaseClearedCallback.onDatabaseCleared();
            }
        }
    }
}
