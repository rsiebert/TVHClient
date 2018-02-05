package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.dao.RecordingDao;
import org.tvheadend.tvhclient.data.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.dao.TimerRecordingDao;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.TimerRecording;

import java.util.List;
import java.util.concurrent.ExecutionException;

// TODO combine methods by using switch statements

public class RecordingRepository {

    private AppDatabase db;

    public RecordingRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public LiveData<List<TimerRecording>> getAllTimerRecordings() {
        return db.timerRecordingDao().loadAllRecordings();
    }

    public LiveData<TimerRecording> getTimerRecording(String id) {
        return db.timerRecordingDao().loadRecordingById(id);
    }

    public TimerRecording getTimerRecordingSync(String id) {
        try {
            return new LoadTimerRecordingTask(db.timerRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Channel getChannelSync(int id) {
        try {
            return new LoadChannelTask(db.channelDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SeriesRecording getSeriesRecordingSync(String id) {
        try {
            return new LoadSeriesRecordingTask(db.seriesRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<SeriesRecording> getSeriesRecording(String id) {
        return db.seriesRecordingDao().loadRecordingById(id);
    }

    public LiveData<List<SeriesRecording>> getAllSeriesRecordings() {
        return db.seriesRecordingDao().loadAllRecordings();
    }

    public Recording getRecordingSync(int id) {
        try {
            return new LoadRecordingTask(db.recordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Recording> getRecording(int id) {
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

    protected static class LoadAllRecordingsTask extends AsyncTask<Void, Void, List<Recording>> {
        private final RecordingDao dao;

        LoadAllRecordingsTask(RecordingDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<Recording> doInBackground(Void... voids) {
            return dao.loadAllRecordings();
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

    protected static class LoadChannelTask extends AsyncTask<Void, Void, Channel> {
        private final ChannelDao dao;
        private final int id;

        LoadChannelTask(ChannelDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Channel doInBackground(Void... voids) {
            return dao.loadChannelByIdSync(id);
        }
    }

    protected static class LoadAllChannelsTask extends AsyncTask<Void, Void, List<Channel>> {
        private final ChannelDao dao;

        LoadAllChannelsTask(ChannelDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return dao.loadAllChannelsSync();
        }
    }
}
