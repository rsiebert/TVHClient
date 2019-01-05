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

public class SeriesRecordingData implements DataSourceInterface<SeriesRecording> {

    private final AppRoomDatabase db;

    @Inject
    public SeriesRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().insert(item));
    }

    @Override
    public void updateItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().update(item));
    }

    @Override
    public void removeItem(SeriesRecording item) {
        AsyncTask.execute(() -> db.getSeriesRecordingDao().delete(item));
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
}
