package org.tvheadend.tvhclient.data;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.dao.ProgramDao;
import org.tvheadend.tvhclient.data.dao.RecordingDao;
import org.tvheadend.tvhclient.data.dao.SeriesRecordingDao;
import org.tvheadend.tvhclient.data.dao.TimerRecordingDao;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.data.model.TimerRecording;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class DataRepository {

    private static DataRepository instance;
    private final AppDatabase appDatabase;
    private final TimerRecordingDao timerRecordingDao;
    private final SeriesRecordingDao seriesRecordingDao;
    private final RecordingDao recordingDao;
    private final ProgramDao programDao;
    private final ChannelDao channelDao;

    private LiveData<List<TimerRecording>> timerRecordings;
    private LiveData<List<SeriesRecording>> seriesRecordings;
    private LiveData<List<Recording>> completedRecordings;
    private LiveData<List<Recording>> scheduledRecordings;
    private LiveData<List<Recording>> failedRecordings;
    private LiveData<List<Recording>> removedRecordings;
    private LiveData<List<Program>> programs;
    private LiveData<List<Channel>> channels;

    public static DataRepository getInstance(final Application application) {
        if (instance == null) {
            synchronized (DataRepository.class) {
                if (instance == null) {
                    instance = new DataRepository(application);
                }
            }
        }
        return instance;
    }

    private DataRepository(Application application) {
        appDatabase = AppDatabase.getInstance(application.getApplicationContext());

        timerRecordingDao = appDatabase.timerRecordingDao();
        seriesRecordingDao = appDatabase.seriesRecordingDao();
        recordingDao = appDatabase.recordingDao();
        programDao = appDatabase.programDao();
        channelDao = appDatabase.channelDao();

        timerRecordings = timerRecordingDao.loadAllRecordings();
        seriesRecordings = seriesRecordingDao.loadAllRecordings();
        completedRecordings = recordingDao.loadAllCompletedRecordings();
        scheduledRecordings = recordingDao.loadAllScheduledRecordings();
        failedRecordings = recordingDao.loadAllFailedRecordings();
        removedRecordings = recordingDao.loadAllRemovedRecordings();
        programs = programDao.loadAllPrograms();
        channels = channelDao.loadAllChannels();
    }

    public LiveData<List<TimerRecording>> getTimerRecordings() {
        return timerRecordings;
    }

    public LiveData<List<SeriesRecording>> getSeriesRecordings() {
        return seriesRecordings;
    }

    public LiveData<List<Recording>> getCompletedRecordings() {
        return completedRecordings;
    }

    public LiveData<List<Recording>> getScheduledRecordings() {
        return scheduledRecordings;
    }

    public LiveData<List<Recording>> getFailedRecordings() {
        return failedRecordings;
    }

    public LiveData<List<Recording>> getRemovedRecordings() {
        return removedRecordings;
    }

    public LiveData<List<Program>> getPrograms() {
        return programs;
    }

    public LiveData<List<Channel>> getChannels() {
        return channels;
    }

    public LiveData<Program> getProgram(int id) {
        return programDao.loadProgram(id);
    }

    public LiveData<Recording> getRecording(int id) {
        return recordingDao.loadRecording(id);
    }

    public LiveData<TimerRecording> getTimerRecording(String id) {
        return timerRecordingDao.loadRecording(id);
    }

    public LiveData<SeriesRecording> getSeriesRecording(String id) {
        return seriesRecordingDao.loadRecording(id);
    }

    public TimerRecording getTimerRecordingFromDatabase(String id) {
        try {
            return new LoadTimerRecordingAsyncTask(appDatabase).execute(id).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SeriesRecording getSeriesRecordingFromDatabase(String id) {
        try {
            return new LoadSeriesRecordingAsyncTask(appDatabase).execute(id).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Program getProgramFromDatabase(int id) {
        try {
            return new LoadProgramAsyncTask(appDatabase).execute(id).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Channel> getChannel(int id) {
        return channelDao.loadChannel(id);
    }

    public Channel getChannelFromDatabase(int id) {
        try {
            return new LoadChannelAsyncTask(appDatabase).execute(id).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearTables() {
        new ClearTablesAsyncTask(appDatabase).execute();
    }

    private static class ClearTablesAsyncTask extends AsyncTask<Void, Void, Void> {
        private AppDatabase db;

        ClearTablesAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.channelDao().deleteAll();
            db.channelTagDao().deleteAll();
            db.programDao().deleteAll();
            db.recordingDao().deleteAll();
            db.seriesRecordingDao().deleteAll();
            db.timerRecordingDao().deleteAll();
            return null;
        }
    }

    private static class LoadTimerRecordingAsyncTask extends AsyncTask<String, Void, TimerRecording> {
        private AppDatabase db;

        LoadTimerRecordingAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected TimerRecording doInBackground(String... strings) {
            return db.timerRecordingDao().loadRecordingSync(strings[0]);
        }
    }

    private static class LoadSeriesRecordingAsyncTask extends AsyncTask<String, Void, SeriesRecording> {
        private AppDatabase db;

        LoadSeriesRecordingAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected SeriesRecording doInBackground(String... strings) {
            return db.seriesRecordingDao().loadRecordingSync(strings[0]);
        }
    }

    private static class LoadProgramAsyncTask extends AsyncTask<Integer, Void, Program> {
        private AppDatabase db;

        LoadProgramAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected Program doInBackground(Integer... integers) {
            return db.programDao().loadProgramSync(integers[0]);
        }
    }

    private static class LoadChannelAsyncTask extends AsyncTask<Integer, Void, Channel> {
        private AppDatabase db;

        LoadChannelAsyncTask(AppDatabase db) {
            this.db = db;
        }

        @Override
        protected Channel doInBackground(Integer... integers) {
            return db.channelDao().loadChannelSync(integers[0]);
        }
    }
}
