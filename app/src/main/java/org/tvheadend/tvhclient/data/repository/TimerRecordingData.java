package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class TimerRecordingData implements DataSourceInterface<TimerRecording> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
    private AppRoomDatabase db;

    @Inject
    public TimerRecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(TimerRecording item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void addItems(List<TimerRecording> items) {
        for (TimerRecording recording : items) {
            addItem(recording);
        }
    }

    @Override
    public void updateItem(TimerRecording item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<TimerRecording> items) {
        for (TimerRecording recording : items) {
            updateItem(recording);
        }
    }

    @Override
    public void removeItem(TimerRecording item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<TimerRecording> items) {
        for (TimerRecording recording : items) {
            removeItem(recording);
        }
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
    public List<TimerRecording> getItems() {
        return null;
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

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final TimerRecording recording;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, TimerRecording recording, int type) {
            this.db = db;
            this.recording = recording;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getTimerRecordingDao().insert(recording);
                    break;
                case UPDATE:
                    db.getTimerRecordingDao().update(recording);
                    break;
                case DELETE:
                    db.getTimerRecordingDao().delete(recording);
                    break;
            }
            return null;
        }
    }
}
