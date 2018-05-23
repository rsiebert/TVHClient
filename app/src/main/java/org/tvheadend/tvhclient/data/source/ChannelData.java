package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelData extends BaseData implements DataSourceInterface<Channel> {

    private AppRoomDatabase db;
    private Context context;

    @Inject
    public ChannelData(AppRoomDatabase database, Context context) {
        this.db = database;
        this.context = context;
    }

    @Override
    public void addItem(Channel item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    public void addItems(List<Channel> items) {
        new ItemsHandlerTask(db, items, INSERT_ALL).execute();
    }

    @Override
    public void updateItem(Channel item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(Channel item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getChannelDao().getChannelCount();
    }

    @Override
    public LiveData<List<Channel>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<Channel> getLiveDataItemById(Object id) {
        return null;
    }

    @Override
    public Channel getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Channel> getItems() {
        try {
            int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
            return new ItemsLoaderTask(db, channelSortOrder).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Channel> getItemByTimeAndTag(long currentTime, int channelTagId) {
        int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
        try {
            return new ItemsLoaderTask(db, currentTime, channelTagId, channelSortOrder).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static class ItemLoaderTask extends AsyncTask<Void, Void, Channel> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected Channel doInBackground(Void... voids) {
            return db.getChannelDao().loadChannelByIdSync(id);
        }
    }

    protected static class ItemsLoaderTask extends AsyncTask<Void, Void, List<Channel>> {
        private final AppRoomDatabase db;
        private final int sortOrder;
        private final long currentTime;
        private final int channelTagId;

        ItemsLoaderTask(AppRoomDatabase db, int sortOrder) {
            this.db = db;
            this.currentTime = 0;
            this.channelTagId = 0;
            this.sortOrder = sortOrder;
        }

        ItemsLoaderTask(AppRoomDatabase db, long currentTime, int channelTagId, int sortOrder) {
            this.db = db;
            this.currentTime = currentTime;
            this.channelTagId = channelTagId;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            if (currentTime == 0) {
                return db.getChannelDao().loadAllChannelsSync(sortOrder);
            } else if (currentTime > 0 && channelTagId == 0) {
                return db.getChannelDao().loadAllChannelsByTimeSync(currentTime, sortOrder);
            } else {
                return db.getChannelDao().loadAllChannelsByTimeAndTagSync(currentTime, channelTagId, sortOrder);
            }
        }
    }

    protected static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final Channel channel;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, Channel channel, int type) {
            this.db = db;
            this.channel = channel;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getChannelDao().insert(channel);
                    break;
                case UPDATE:
                    db.getChannelDao().update(channel);
                    break;
                case DELETE:
                    db.getChannelDao().delete(channel);
                    break;
            }
            return null;
        }
    }

    protected static class ItemsHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final List<Channel> channels;
        private final int type;

        ItemsHandlerTask(AppRoomDatabase db, List<Channel> channels, int type) {
            this.db = db;
            this.channels = channels;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT_ALL:
                    db.getChannelDao().insert(channels);
                    break;
            }
            return null;
        }
    }
}
