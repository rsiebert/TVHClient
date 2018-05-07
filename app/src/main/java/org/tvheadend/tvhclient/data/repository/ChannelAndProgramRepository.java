package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.dao.ChannelDao;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.features.channels.ChannelsLoadedCallback;

import java.util.List;

public class ChannelAndProgramRepository {
    private final SharedPreferences sharedPreferences;
    private AppRoomDatabase db;

    public ChannelAndProgramRepository(Context context) {
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void getAllChannelsByTimeAndTagSync(long currentTime, int channelTagId, ChannelsLoadedCallback callback) {
        int channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
        new LoadAllChannelsByTimeAndTagTask(db.getChannelDao(), currentTime, channelTagId, channelSortOrder, callback).execute();
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

}
