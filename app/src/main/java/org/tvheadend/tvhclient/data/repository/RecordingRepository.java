package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.local.dao.RecordingDao;
import org.tvheadend.tvhclient.data.local.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.local.dao.TimerRecordingDao;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.TimerRecording;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class RecordingRepository {

    private AppRoomDatabase db;

    public RecordingRepository(Context context) {
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
    }

    public LiveData<List<TimerRecording>> getAllTimerRecordings() {
        return db.getTimerRecordingDao().loadAllRecordings();
    }

    public LiveData<TimerRecording> getTimerRecordingById(String id) {
        return db.getTimerRecordingDao().loadRecordingById(id);
    }

    public TimerRecording getTimerRecordingByIdSync(String id) {
        try {
            return new LoadTimerRecordingTask(db.getTimerRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SeriesRecording getSeriesRecordingByIdSync(String id) {
        try {
            return new LoadSeriesRecordingTask(db.getSeriesRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<SeriesRecording> getSeriesRecordingById(String id) {
        return db.getSeriesRecordingDao().loadRecordingById(id);
    }

    public LiveData<List<SeriesRecording>> getAllSeriesRecordings() {
        return db.getSeriesRecordingDao().loadAllRecordings();
    }

    public Recording getRecordingByIdSync(int id) {
        try {
            return new LoadRecordingTask(db.getRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Recording> getRecordingById(int id) {
        return db.getRecordingDao().loadRecordingById(id);
    }

    public LiveData<List<Recording>> getAllCompletedRecordings() {
        return db.getRecordingDao().loadAllCompletedRecordings();
    }

    public LiveData<List<Recording>> getAllScheduledRecordings() {
        return db.getRecordingDao().loadAllScheduledRecordings();
    }

    public LiveData<List<Recording>> getAllFailedRecordings() {
        return db.getRecordingDao().loadAllFailedRecordings();
    }

    public LiveData<List<Recording>> getAllRemovedRecordings() {
        return db.getRecordingDao().loadAllRemovedRecordings();
    }

    public LiveData<List<Recording>> getAllRecordingsByChannelId(int channelId) {
        return db.getRecordingDao().loadAllRecordingsByChannelId(channelId);
    }

    public LiveData<List<Recording>> getAllRecordings() {
        return db.getRecordingDao().loadAllRecordings();
    }

    public Recording getRecordingByEventIdSync(int eventId) {
        try {
            return new LoadRecordingByEventIdTask(db.getRecordingDao(), eventId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Integer> getNumberOfSeriesRecordings() {
        return db.getSeriesRecordingDao().getRecordingCount();
    }

    public LiveData<Integer> getNumberOfTimerRecordings() {
        return db.getTimerRecordingDao().getRecordingCount();
    }

    public LiveData<Integer> getNumberOfCompletedRecordings() {
        return db.getRecordingDao().getCompletedRecordingCount();
    }

    public LiveData<Integer> getNumberOfScheduledRecordings() {
        return db.getRecordingDao().getScheduledRecordingCount();
    }

    public LiveData<Integer> getNumberOfFailedRecordings() {
        return db.getRecordingDao().getFailedRecordingCount();
    }

    public LiveData<Integer> getNumberOfRemovedRecordings() {
        return db.getRecordingDao().getRemovedRecordingCount();
    }

    protected static class LoadRecordingByEventIdTask extends AsyncTask<Void, Void, Recording> {
        private final RecordingDao dao;
        private final int id;

        LoadRecordingByEventIdTask(RecordingDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Recording doInBackground(Void... voids) {
            return dao.loadRecordingByEventIdSync(id);
        }
    }

    protected static class LoadRecordingTask extends AsyncTask<Void, Void, Recording> {
        private final RecordingDao dao;
        private final int id;

        LoadRecordingTask(RecordingDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Recording doInBackground(Void... voids) {
            return dao.loadRecordingByIdSync(id);
        }
    }

    protected static class LoadTimerRecordingTask extends AsyncTask<Void, Void, TimerRecording> {
        private final TimerRecordingDao dao;
        private final String id;

        LoadTimerRecordingTask(TimerRecordingDao dao, String id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected TimerRecording doInBackground(Void... voids) {
            return dao.loadRecordingByIdSync(id);
        }
    }

    protected static class LoadSeriesRecordingTask extends AsyncTask<Void, Void, SeriesRecording> {
        private final SeriesRecordingDao dao;
        private final String id;

        LoadSeriesRecordingTask(SeriesRecordingDao dao, String id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected SeriesRecording doInBackground(Void... voids) {
            return dao.loadRecordingByIdSync(id);
        }
    }
}
