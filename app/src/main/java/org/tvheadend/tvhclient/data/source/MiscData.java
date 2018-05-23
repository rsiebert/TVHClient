package org.tvheadend.tvhclient.data.source;

import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import javax.inject.Inject;

public class MiscData extends BaseData {

    private AppRoomDatabase db;

    @Inject
    public MiscData(AppRoomDatabase database) {
        this.db = database;
    }

    public void clearDatabase() {
        new ClearDatabaseTask(db).execute();
    }

    protected static class ClearDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;

        ClearDatabaseTask(AppRoomDatabase db) {
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
            return null;
        }
    }
}
