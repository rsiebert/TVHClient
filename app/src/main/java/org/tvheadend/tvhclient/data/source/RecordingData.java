package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class RecordingData implements DataSourceInterface<Recording> {

    private static final int LOAD_BY_ID = 1;
    private static final int LOAD_BY_EVENT_ID = 2;

    private final AppRoomDatabase db;

    @Inject
    public RecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Recording item) {
        AsyncTask.execute(() -> db.getRecordingDao().insert(item));
    }

    public void addItems(@NonNull List<Recording> items) {
        AsyncTask.execute(() -> db.getRecordingDao().insert(items));
    }

    @Override
    public void updateItem(Recording item) {
        AsyncTask.execute(() -> db.getRecordingDao().update(item));
    }

    @Override
    public void removeItem(Recording item) {
        AsyncTask.execute(() -> db.getRecordingDao().delete(item));
    }

    public void removeItems() {
        db.getRecordingDao().deleteAll();
    }

    public void replaceItems(@NonNull List<Recording> items) {
        db.getRecordingDao().deleteAll();
        db.getRecordingDao().insert(items);
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
            return new ItemLoaderTask(db, (int) id, LOAD_BY_ID).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<Recording> getItems() {
        return new ArrayList<>();
    }

    public LiveData<List<Recording>> getLiveDataItemsByChannelId(int channelId) {
        return db.getRecordingDao().loadAllRecordingsByChannelId(channelId);
    }

    public Recording getItemByEventId(int id) {
        try {
            return new ItemLoaderTask(db, id, LOAD_BY_EVENT_ID).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, Recording> {
        private final AppRoomDatabase db;
        private final int id;
        private final int type;

        ItemLoaderTask(AppRoomDatabase db, int id, int type) {
            this.db = db;
            this.id = id;
            this.type = type;
        }

        @Override
        protected Recording doInBackground(Void... voids) {
            switch (type) {
                case LOAD_BY_EVENT_ID:
                    return db.getRecordingDao().loadRecordingByEventIdSync(id);
                case LOAD_BY_ID:
                    return db.getRecordingDao().loadRecordingByIdSync(id);
            }
            return null;
        }
    }
}
