package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelData implements DataSourceInterface<Channel> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
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

    @Override
    public void addItems(List<Channel> items) {
        for (Channel channel : items) {
            addItem(channel);
        }
    }

    @Override
    public void updateItem(Channel item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<Channel> items) {
        for (Channel channel : items) {
            updateItem(channel);
        }
    }

    @Override
    public void removeItem(Channel item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<Channel> items) {
        for (Channel channel : items) {
            removeItem(channel);
        }
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

        ItemsLoaderTask(AppRoomDatabase db, int sortOrder) {
            this.db = db;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return db.getChannelDao().loadAllChannelsSync(sortOrder);
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
}
