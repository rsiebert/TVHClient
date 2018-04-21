package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.local.dao.ChannelDao;
import org.tvheadend.tvhclient.data.local.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.local.dao.ProgramDao;
import org.tvheadend.tvhclient.data.local.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

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
            return new LoadAllChannelsTask(db.channelDao(), channelSortOrder).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ChannelTag> getAllChannelTagsSync() {
        try {
            return new LoadAllChannelTagsTask(db.channelTagDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Channel getChannelByIdSync(int channelId) {
        try {
            return new LoadChannelByIdTask(db.channelDao(), channelId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Program getProgramByIdSync(int eventId) {
        try {
            return new LoadProgramByIdTask(db.programDao(), eventId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<Program>> getProgramsByChannelFromTime(int channelId, long time) {
        return db.programDao().loadProgramsFromChannelWithinTime(channelId, time);
    }

    public List<Channel> getAllChannelsByTimeAndTagSync(long currentTime, int channelTagId) {
        try {
            int channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
            return new LoadAllChannelsTask(db.channelDao(), currentTime, channelTagId, channelSortOrder).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ChannelTag getChannelTagByIdSync(int channelTagId) {
        try {
            return new LoadChannelTagTask(db.channelTagDao(), channelTagId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Integer> getNumberOfChannels() {
        return db.channelDao().getChannelCount();
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
        private final long currentTime;
        private final int channelTagId;
        private final int sortOrder;

        LoadAllChannelsTask(ChannelDao dao, long currentTime, int channelTagId, int sortOrder) {
            this.dao = dao;
            this.currentTime = currentTime;
            this.channelTagId = channelTagId;
            this.sortOrder = sortOrder;
        }

        LoadAllChannelsTask(ChannelDao dao, int sortOrder) {
            this.dao = dao;
            this.currentTime = 0;
            this.channelTagId = 0;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            if (currentTime == 0) {
                Timber.d("doInBackground: currentTime is 0");
                return dao.loadAllChannelsSync(sortOrder);
            } else if (currentTime > 0 && channelTagId == 0) {
                Timber.d("doInBackground: currentTime > 0 and channelTagId is 0");
                List<Channel> channels = dao.loadAllChannelsByTime(currentTime, sortOrder);

                for (Channel c : channels) {
                    Timber.d("Found channel " + c.getName() + " with id " + c.getId());
                }
                return channels;
            } else {
                Timber.d("doInBackground: currentTime > 0 and channelTagId > 0");
                return dao.loadAllChannelsByTimeAndTag(currentTime, channelTagId, sortOrder);
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
            return new LoadServerStatusTask(db.serverStatusDao()).execute().get();
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
