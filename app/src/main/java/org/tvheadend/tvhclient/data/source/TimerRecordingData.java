package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class TimerRecordingData implements DataSourceInterface<TimerRecording> {

    private final AppRoomDatabase db;

    @Inject
    public TimerRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(TimerRecording item) {
        AsyncTask.execute(() -> db.getTimerRecordingDao().insert(item));
    }

    @Override
    public void updateItem(TimerRecording item) {
        AsyncTask.execute(() -> db.getTimerRecordingDao().update(item));
    }

    @Override
    public void removeItem(TimerRecording item) {
        AsyncTask.execute(() -> db.getTimerRecordingDao().delete(item));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getTimerRecordingDao().getRecordingCount();
    }

    public LiveData<List<TimerRecording>> getLiveDataItems() {
        return db.getTimerRecordingDao().loadAllRecordings();
    }

    @Override
    public LiveData<TimerRecording> getLiveDataItemById(Object id) {
        return db.getTimerRecordingDao().loadRecordingById((String) id);
    }

    @Override
    public TimerRecording getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (String) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<TimerRecording> getItems() {
        return new ArrayList<>();
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, TimerRecording> {
        private final AppRoomDatabase db;
        private final String id;

        ItemLoaderTask(AppRoomDatabase db, String id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected TimerRecording doInBackground(Void... voids) {
            return db.getTimerRecordingDao().loadRecordingByIdSync(id);
        }
    }
}
