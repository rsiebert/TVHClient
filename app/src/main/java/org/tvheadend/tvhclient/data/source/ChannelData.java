package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelData extends BaseData implements DataSourceInterface<Channel> {

    private final AppRoomDatabase db;
    private final Context context;

    @Inject
    public ChannelData(AppRoomDatabase database, Context context) {
        this.db = database;
        this.context = context;
    }

    @Override
    public void addItem(Channel item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    public void addItems(@NonNull List<Channel> items) {
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
    @NonNull
    public List<Channel> getItems() {
        List<Channel> channels = new ArrayList<>();
        try {
            int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
            channels.addAll(new ItemsLoaderTask(db, channelSortOrder).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return channels;
    }

    @NonNull
    public List<Channel> getItemsByTimeAndTag(long currentTime, int channelTagId) {
        int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
        List<Channel> channels = new ArrayList<>();
        try {
            channels.addAll(new ItemsLoaderTask(db, currentTime, channelTagId, channelSortOrder).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return channels;
    }

    public List<ChannelSubset> getChannelNamesByTimeAndTag(int channelTagId) {
        int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
        List<ChannelSubset> channels = new ArrayList<>();
        try {
            channels.addAll(new ItemSubsetsLoaderTask(db, channelTagId, channelSortOrder).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return channels;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, Channel> {
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

    private static class ItemsLoaderTask extends AsyncTask<Void, Void, List<Channel>> {
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

    private static class ItemSubsetsLoaderTask extends AsyncTask<Void, Void, List<ChannelSubset>> {
        private final AppRoomDatabase db;
        private final int sortOrder;
        private final int channelTagId;

        ItemSubsetsLoaderTask(AppRoomDatabase db, int channelTagId, int sortOrder) {
            this.db = db;
            this.channelTagId = channelTagId;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<ChannelSubset> doInBackground(Void... voids) {
            if (channelTagId == 0) {
                return db.getChannelDao().loadAllChannelsNamesOnlySync(sortOrder);
            } else {
                return db.getChannelDao().loadAllChannelsNamesOnlyByTagSync(channelTagId, sortOrder);
            }
        }
    }

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
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

    private static class ItemsHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final List<Channel> channels;
        private final int type;

        ItemsHandlerTask(AppRoomDatabase db, List<Channel> channels, int type) {
            this.db = db;
            this.channels = new CopyOnWriteArrayList<>(channels);
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
