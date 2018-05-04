package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class RecordingData implements DataSourceInterface<Recording> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
    private AppRoomDatabase db;

    @Inject
    public RecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Recording item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void addItems(List<Recording> items) {
        for (Recording recording : items) {
            addItem(recording);
        }
    }

    @Override
    public void updateItem(Recording item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<Recording> items) {
        for (Recording recording : items) {
            updateItem(recording);
        }
    }

    @Override
    public void removeItem(Recording item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<Recording> items) {
        for (Recording recording : items) {
            removeItem(recording);
        }
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    public LiveData<List<Recording>> getLiveDataItems() {
        return db.getRecordingDao().loadAllRecordings();
    }

    @Override
    public LiveData<Recording> getLiveDataItemById(Object id) {
        return db.getRecordingDao().loadRecordingById((int) id);
    }

    public LiveData<List<Recording>> getLiveDataItemsByType(String type) {
        switch (type) {
            case "completed":
                return db.getRecordingDao().loadAllCompletedRecordings();
            case "scheduled":
                return db.getRecordingDao().loadAllScheduledRecordings();
            case "failed":
                return db.getRecordingDao().loadAllFailedRecordings();
            case "removed":
                return db.getRecordingDao().loadAllRemovedRecordings();
            default:
                return null;
        }
    }

    public LiveData<Integer> getLiveDataCountByType(String type) {
        switch (type) {
            case "completed":
                return db.getRecordingDao().getCompletedRecordingCount();
            case "scheduled":
                return db.getRecordingDao().getScheduledRecordingCount();
            case "failed":
                return db.getRecordingDao().getFailedRecordingCount();
            case "removed":
                return db.getRecordingDao().getRemovedRecordingCount();
            default:
                return null;
        }
    }

    @Override
    public Recording getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, Recording> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected Recording doInBackground(Void... voids) {
            return db.getRecordingDao().loadRecordingByIdSync(id);
        }
    }

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final Recording recording;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, Recording recording, int type) {
            this.db = db;
            this.recording = recording;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getRecordingDao().insert(recording);
                    break;
                case UPDATE:
                    db.getRecordingDao().update(recording);
                    break;
                case DELETE:
                    db.getRecordingDao().delete(recording);
                    break;
            }
            return null;
        }
    }
}
