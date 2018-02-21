package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.dao.ChannelTagDao;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChannelAndProgramRepository {
    private String TAG = getClass().getSimpleName();

    private AppDatabase db;

    public ChannelAndProgramRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }


    public List<Channel> getAllChannelsSync() {
        try {
            return new LoadAllChannelsTask(db.channelDao()).execute().get();
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

    public LiveData<List<Channel>> getAllChannelsByTimeAndTag() {
        Log.d(TAG, "getAllChannelsByTimeAndTag() called");
        ServerStatus serverStatus = getServerStatus();
        if (serverStatus.getChannelTagId() > 0) {
            Log.d(TAG, "getAllChannelsByTimeAndTag: tag id > 0");
            return db.channelDao().loadAllChannelsByTimeAndTag(serverStatus.getChannelTagId());
        } else {
            Log.d(TAG, "getAllChannelsByTimeAndTag: tag is is 0");
            return db.channelDao().loadAllChannelsByTime();
        }
    }

    public Channel getChannelByIdSync(int channelId) {
        try {
            return new LoadChannelByIdTask(db.channelDao(), channelId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateChannelTime(long time) {
        Log.d(TAG, "updateChannelTime() called with: time = [" + time + "]");
        new UpdateChannelTask(db.channelDao(), time).execute();
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

        LoadAllChannelsTask(ChannelDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return dao.loadAllChannelsSync();
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

    private static class UpdateChannelTask extends AsyncTask<Long, Void, Void> {
        private final ChannelDao dao;
        private final long time;

        public UpdateChannelTask(ChannelDao dao, long time) {
            this.dao = dao;
            this.time = time;
        }

        @Override
        protected Void doInBackground(Long... longs) {
            dao.updateCurrenTime(time);
            return null;
        }
    }
}
