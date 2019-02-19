package org.tvheadend.tvhclient.domain.repository.data_source;

import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.Recording;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import timber.log.Timber;

public class RecordingData implements DataSourceInterface<Recording> {

    private static final int LOAD_BY_ID = 1;
    private static final int LOAD_BY_EVENT_ID = 2;

    private final AppRoomDatabase db;

    public RecordingData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Recording item) {
        AsyncTask.execute(() -> db.getRecordingDao().insert(item));
    }

    public void addItems(@NonNull List<Recording> items) {
        AsyncTask.execute(() -> db.getRecordingDao().insert(new ArrayList<>(items)));
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
        AsyncTask.execute(() -> db.getRecordingDao().deleteAll());
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    public LiveData<List<Recording>> getLiveDataItems() {
        return db.getRecordingDao().loadAllRecordings();
    }

    @Override
    public LiveData<Recording> getLiveDataItemById(@NonNull Object id) {
        return db.getRecordingDao().loadRecordingById((int) id);
    }

    public LiveData<List<Recording>> getLiveDataItemsByChannelId(int channelId) {
        return db.getRecordingDao().loadAllRecordingsByChannelId(channelId);
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
    public Recording getItemById(@NonNull Object id) {
        try {
            return new RecordingByIdTask(db, (int) id, LOAD_BY_ID).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading recording by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading recording by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<Recording> getItems() {
        return new ArrayList<>();
    }

    public int getItemCount() {
        try {
            return new RecordingCountTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading recording count task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading recording count task aborted", e);
        }
        return 0;
    }

    @Nullable
    public Recording getItemByEventId(int id) {
        try {
            return new RecordingByIdTask(db, id, LOAD_BY_EVENT_ID).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading recording by event id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading recording by event id task aborted", e);
        }
        return null;
    }

    private static class RecordingByIdTask extends AsyncTask<Void, Void, Recording> {
        private final AppRoomDatabase db;
        private final int id;
        private final int type;

        RecordingByIdTask(AppRoomDatabase db, int id, int type) {
            this.db = db;
            this.id = id;
            this.type = type;
        }

        @Override
        protected Recording doInBackground(Void... voids) {
            if (type == LOAD_BY_ID) {
                return db.getRecordingDao().loadRecordingByIdSync(id);
            } else if (type == LOAD_BY_EVENT_ID) {
                return db.getRecordingDao().loadRecordingByEventIdSync(id);
            } else {
                return null;
            }
        }
    }

    private static class RecordingCountTask extends AsyncTask<Void, Void, Integer> {
        private final AppRoomDatabase db;

        RecordingCountTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return db.getRecordingDao().getItemCountSync();
        }
    }
}
