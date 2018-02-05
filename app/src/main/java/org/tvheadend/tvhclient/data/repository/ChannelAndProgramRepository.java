package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;

import java.util.List;
import java.util.concurrent.ExecutionException;

// TODO methods to get and set active channel tag

public class ChannelAndProgramRepository {

    private AppDatabase db;

    public ChannelAndProgramRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }


    public List<Channel> getAllChannelsSync() {
        try {
            return new RecordingRepository.LoadAllChannelsTask(db.channelDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ChannelTag> getAllChannelTags() {
        try {
            return new LoadAllChannelTagsTask(db.channelTagDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<Channel>> getAllChannels() {
        return db.channelDao().loadAllChannels();
    }

    public List<Channel> getAllChannelsByTimeSync(long showProgramsFromTime) {
        try {
            return new LoadAllChannelsByTimeTask(db.channelDao(), showProgramsFromTime).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Channel getChannelByIdSync(int channelId) {
        try {
            return new LoadChannelsByIdTask(db.channelDao(), channelId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static class LoadAllChannelTagsTask extends AsyncTask<Void, Void, List<ChannelTag>> {
        private final ChannelTagDao dao;

        LoadAllChannelTagsTask(ChannelTagDao dao) {
            this.dao = dao;        }

        @Override
        protected List<ChannelTag> doInBackground(Void... voids) {
            return dao.loadAllChannelTagsSync();
        }
    }

    private static class LoadAllChannelsByTimeTask extends AsyncTask<Void, Void, List<Channel>> {
        private final ChannelDao dao;
        private final long time;

        LoadAllChannelsByTimeTask(ChannelDao dao, long time) {
            this.dao = dao;
            this.time = time;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return dao.loadAllChannelsByTimeSync(time);
        }
    }

    private static class LoadChannelsByIdTask extends AsyncTask<Void, Void, Channel> {
        private final ChannelDao dao;
        private final int id;

        LoadChannelsByIdTask(ChannelDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Channel doInBackground(Void... voids) {
            return dao.loadChannelByIdSync(id);
        }
    }
}
