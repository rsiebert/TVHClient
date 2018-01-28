package org.tvheadend.tvhclient.data;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.TVHClientApplication;
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

public class DataRepository {

    private AppDatabase db;

    public DataRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public LiveData<List<TimerRecording>> getAllTimerRecordings() {
        return db.timerRecordingDao().loadAllRecordings();
    }

    public LiveData<TimerRecording> getTimerRecording(String id) {
        return db.timerRecordingDao().loadRecording(id);
    }

    public TimerRecording getTimerRecordingSync(String id) {
        try {
            return new LoadTimerRecordingTask(db.timerRecordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Channel> getAllChannelsSync() {
        try {
            return new LoadAllChannelsTask(db.channelDao()).execute().get();
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
        return db.seriesRecordingDao().loadRecording(id);
    }

    public LiveData<List<SeriesRecording>> getAllSeriesRecordings() {
        return db.seriesRecordingDao().loadAllRecordings();
    }

    public int getHtspVersion() {
        // TODO
        return 30;
    }

    public Recording getRecordingSync(int id) {
        try {
            return new LoadRecordingTask(db.recordingDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean getIsUnlocked() {
        // TODO use room database
        return TVHClientApplication.getInstance().isUnlocked();
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
            return dao.loadRecordingSync(id);
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
            return dao.loadRecordingSync(id);
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
            return dao.loadRecordingSync(id);
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
            return dao.loadChannelSync(id);
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
