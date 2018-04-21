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
        return db.timerRecordingDao().loadAllRecordings();
    }

    public LiveData<TimerRecording> getTimerRecordingById(String id) {
        return db.timerRecordingDao().loadRecordingById(id);
    }

    public TimerRecording getTimerRecordingByIdSync(String id) {
        try {
            return new LoadTimerRecordingTask(db.timerRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SeriesRecording getSeriesRecordingByIdSync(String id) {
        try {
            return new LoadSeriesRecordingTask(db.seriesRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<SeriesRecording> getSeriesRecordingById(String id) {
        return db.seriesRecordingDao().loadRecordingById(id);
    }

    public LiveData<List<SeriesRecording>> getAllSeriesRecordings() {
        return db.seriesRecordingDao().loadAllRecordings();
    }

    public Recording getRecordingByIdSync(int id) {
        try {
            return new LoadRecordingTask(db.recordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Recording> getRecordingById(int id) {
        return db.recordingDao().loadRecordingById(id);
    }

    public LiveData<List<Recording>> getAllCompletedRecordings() {
        return db.recordingDao().loadAllCompletedRecordings();
    }

    public LiveData<List<Recording>> getAllScheduledRecordings() {
        return db.recordingDao().loadAllScheduledRecordings();
    }

    public LiveData<List<Recording>> getAllFailedRecordings() {
        return db.recordingDao().loadAllFailedRecordings();
    }

    public LiveData<List<Recording>> getAllRemovedRecordings() {
        return db.recordingDao().loadAllRemovedRecordings();
    }

    public List<Recording> getAllRecordingsSync() {
        try {
            return new LoadAllRecordingsTask(db.recordingDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<Recording>> getAllRecordingsByChannelId(int channelId) {
        return db.recordingDao().loadAllRecordingsByChannelId(channelId);
    }

    public LiveData<List<Recording>> getAllRecordings() {
        return db.recordingDao().loadAllRecordings();
    }

    public Recording getRecordingByEventIdSync(int eventId) {
        try {
            return new LoadRecordingByEventIdTask(db.recordingDao(), eventId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Integer> getNumberOfSeriesRecordings() {
        return db.seriesRecordingDao().getRecordingCount();
    }

    public LiveData<Integer> getNumberOfTimerRecordings() {
        return db.timerRecordingDao().getRecordingCount();
    }

    public LiveData<Integer> getNumberOfCompletedRecordings() {
        return db.recordingDao().getCompletedRecordingCount();
    }

    public LiveData<Integer> getNumberOfScheduledRecordings() {
        return db.recordingDao().getScheduledRecordingCount();
    }

    public LiveData<Integer> getNumberOfFailedRecordings() {
        return db.recordingDao().getFailedRecordingCount();
    }

    public LiveData<Integer> getNumberOfRemovedRecordings() {
        return db.recordingDao().getRemovedRecordingCount();
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

    protected static class LoadAllRecordingsTask extends AsyncTask<Void, Void, List<Recording>> {
        private final RecordingDao dao;

        LoadAllRecordingsTask(RecordingDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<Recording> doInBackground(Void... voids) {
            return dao.loadAllRecordingsSync();
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
