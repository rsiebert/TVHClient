package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class SeriesRecordingData extends BaseData implements DataSourceInterface<SeriesRecording> {

    final private AppRoomDatabase db;

    @Inject
    public SeriesRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void updateItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(SeriesRecording item) {
        new ItemHandlerTask(db, item, DELETE).execute();
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

    @Override
    @NonNull
    public List<SeriesRecording> getItems() {
        return new ArrayList<>();
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
