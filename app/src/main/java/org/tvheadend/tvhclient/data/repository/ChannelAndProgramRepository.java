package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.local.dao.ChannelDao;
import org.tvheadend.tvhclient.data.local.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.local.dao.ProgramDao;
import org.tvheadend.tvhclient.data.local.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.features.channels.ChannelsLoadedCallback;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChannelAndProgramRepository {
    private final SharedPreferences sharedPreferences;
    private AppRoomDatabase db;

    public ChannelAndProgramRepository(Context context) {
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public List<Channel> getAllChannelsSync() {
        try {
            int channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
            return new LoadAllChannelsTask(db.getChannelDao(), channelSortOrder).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ChannelTag> getAllChannelTagsSync() {
        try {
            return new LoadAllChannelTagsTask(db.getChannelTagDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Channel getChannelByIdSync(int channelId) {
        try {
            return new LoadChannelByIdTask(db.getChannelDao(), channelId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Program getProgramByIdSync(int eventId) {
        try {
            return new LoadProgramByIdTask(db.getProgramDao(), eventId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<Program>> getProgramsByChannelFromTime(int channelId, long time) {
        return db.getProgramDao().loadProgramsFromChannelWithinTime(channelId, time);
    }

    public void getAllChannelsByTimeAndTagSync(long currentTime, int channelTagId, ChannelsLoadedCallback callback) {
        int channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
        new LoadAllChannelsByTimeAndTagTask(db.getChannelDao(), currentTime, channelTagId, channelSortOrder, callback).execute();
    }

    public ChannelTag getChannelTagByIdSync(int channelTagId) {
        try {
            return new LoadChannelTagTask(db.getChannelTagDao(), channelTagId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Integer> getNumberOfChannels() {
        return db.getChannelDao().getChannelCount();
    }

    private static class LoadProgramByIdTask extends AsyncTask<Void, Void, Program> {
        private final ProgramDao dao;
        private final int id;

        LoadProgramByIdTask(ProgramDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Program doInBackground(Void... voids) {
            return dao.loadProgramByIdSync(id);
        }
    }

    protected static class LoadChannelTagTask extends AsyncTask<Void, Void, ChannelTag> {
        private final ChannelTagDao dao;
        private int channelTagId;

        LoadChannelTagTask(ChannelTagDao dao, int channelTagId) {
            this.dao = dao;
            this.channelTagId = channelTagId;
        }

        @Override
        protected ChannelTag doInBackground(Void... voids) {
            return dao.loadChannelTagByIdSync(channelTagId);
        }
    }

    protected static class LoadAllChannelTagsTask extends AsyncTask<Void, Void, List<ChannelTag>> {
        private final ChannelTagDao dao;

        LoadAllChannelTagsTask(ChannelTagDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<ChannelTag> doInBackground(Void... voids) {
            return dao.loadAllChannelTagsSync();
        }
    }

    private static class LoadAllChannelsTask extends AsyncTask<Void, Void, List<Channel>> {

        private final ChannelDao dao;
        private final int sortOrder;

        LoadAllChannelsTask(ChannelDao dao, int sortOrder) {
            this.dao = dao;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return dao.loadAllChannelsSync(sortOrder);
        }
    }

    private static class LoadAllChannelsByTimeAndTagTask extends AsyncTask<Void, Void, List<Channel>> {

        private final ChannelDao dao;
        private final long currentTime;
        private final int channelTagId;
        private final int sortOrder;
        private final ChannelsLoadedCallback callback;

        LoadAllChannelsByTimeAndTagTask(ChannelDao dao, long currentTime, int channelTagId, int sortOrder, ChannelsLoadedCallback callback) {
            this.dao = dao;
            this.currentTime = currentTime;
            this.channelTagId = channelTagId;
            this.sortOrder = sortOrder;
            this.callback = callback;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            if (currentTime == 0) {
                return dao.loadAllChannelsSync(sortOrder);
            } else if (currentTime > 0 && channelTagId == 0) {
                return dao.loadAllChannelsByTimeSync(currentTime, sortOrder);
            } else {
                return dao.loadAllChannelsByTimeAndTagSync(currentTime, channelTagId, sortOrder);
            }
        }

        @Override
        protected void onPostExecute(List<Channel> channels) {
            if (callback != null) {
                callback.onChannelsLoaded(channels);
            }
        }
    }

    private static class LoadChannelByIdTask extends AsyncTask<Void, Void, Channel> {
        private final ChannelDao dao;
        private final int id;

        LoadChannelByIdTask(ChannelDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Channel doInBackground(Void... voids) {
            return dao.loadChannelByIdSync(id);
        }
    }

    public ServerStatus getServerStatus() {
        try {
            return new LoadServerStatusTask(db.getServerStatusDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class LoadServerStatusTask extends AsyncTask<Void, Void, ServerStatus> {
        private final ServerStatusDao dao;

        LoadServerStatusTask(ServerStatusDao dao) {
            this.dao = dao;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {
            return dao.loadServerStatusSync();
        }
    }
}
