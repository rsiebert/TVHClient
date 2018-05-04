package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class SeriesRecordingData implements DataSourceInterface<SeriesRecording> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
    private AppRoomDatabase db;

    @Inject
    public SeriesRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void addItems(List<SeriesRecording> items) {
        for (SeriesRecording recording : items) {
            addItem(recording);
        }
    }

    @Override
    public void updateItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<SeriesRecording> items) {
        for (SeriesRecording recording : items) {
            updateItem(recording);
        }
    }

    @Override
    public void removeItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<SeriesRecording> items) {
        for (SeriesRecording recording : items) {
            removeItem(recording);
        }
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getSeriesRecordingDao().getRecordingCount();
    }

    public LiveData<List<SeriesRecording>> getLiveDataItems() {
        return db.getSeriesRecordingDao().loadAllRecordings();
    }

    @Override
    public LiveData<SeriesRecording> getLiveDataItemById(Object id) {
        return db.getSeriesRecordingDao().loadRecordingById((String) id);
    }

    @Override
    public SeriesRecording getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (String) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, SeriesRecording> {
        private final AppRoomDatabase db;
        private final String id;

        ItemLoaderTask(AppRoomDatabase db, String id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected SeriesRecording doInBackground(Void... voids) {
            return db.getSeriesRecordingDao().loadRecordingByIdSync(id);
        }
    }

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final SeriesRecording recording;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, SeriesRecording recording, int type) {
            this.db = db;
            this.recording = recording;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getSeriesRecordingDao().insert(recording);
                    break;
                case UPDATE:
                    db.getSeriesRecordingDao().update(recording);
                    break;
                case DELETE:
                    db.getSeriesRecordingDao().delete(recording);
                    break;
            }
            return null;
        }
    }
}
