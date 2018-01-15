package org.tvheadend.tvhclient.data.db;

import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import org.tvheadend.tvhclient.data.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.dao.TimerRecordingDao;

public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public static AppDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "tvhclient_db")
                    .build();
        }
        return instance;
    }

    public abstract TimerRecordingDao timerRecordingDao();
    public abstract SeriesRecordingDao seriesRecordingDao();
}
