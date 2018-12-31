package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.EpgChannel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public List<Channel> getItemsByTimeAndTags(long currentTime, Set<Integer> channelTagIds) {
        int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
        List<Channel> channels = new ArrayList<>();
        try {
            channels.addAll(new ItemsLoaderTask(db, currentTime, channelTagIds, channelSortOrder).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return channels;
    }

    public List<EpgChannel> getChannelNamesByTag(Set<Integer> channelTagIds) {
        int channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", "0"));
        List<EpgChannel> channels = new ArrayList<>();
        try {
            channels.addAll(new ItemSubsetsLoaderTask(db, channelTagIds, channelSortOrder).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return channels;
    }

    public Channel getItemByIdWithPrograms(int id, long selectedTime) {
        try {
            return new ItemLoaderTask(db, id, selectedTime).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, Channel> {
        private final AppRoomDatabase db;
        private final int id;
        private final long selectedTime;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
            this.selectedTime = 0;
        }

        ItemLoaderTask(AppRoomDatabase db, int id, long selectedTime) {
            this.db = db;
            this.id = id;
            this.selectedTime = selectedTime;
        }

        @Override
        protected Channel doInBackground(Void... voids) {
            if (selectedTime > 0) {
                return db.getChannelDao().loadChannelByIdWithProgramsSync(id, selectedTime);
            } else {
                return db.getChannelDao().loadChannelByIdSync(id);
            }
        }
    }

    private static class ItemsLoaderTask extends AsyncTask<Void, Void, List<Channel>> {
        private final AppRoomDatabase db;
        private final int sortOrder;
        private final long currentTime;
        private final Set<Integer> channelTagIds;

        ItemsLoaderTask(AppRoomDatabase db, int sortOrder) {
            this.db = db;
            this.currentTime = 0;
            this.channelTagIds = new HashSet<>();
            this.sortOrder = sortOrder;
        }

        ItemsLoaderTask(AppRoomDatabase db, long currentTime, Set<Integer> channelTagIds, int sortOrder) {
            this.db = db;
            this.currentTime = currentTime;
            this.channelTagIds = channelTagIds;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            if (currentTime == 0) {
                return db.getChannelDao().loadAllChannelsSync(sortOrder);
            } else if (currentTime > 0 && channelTagIds.size() == 0) {
                return db.getChannelDao().loadAllChannelsByTimeSync(currentTime, sortOrder);
            } else {
                return db.getChannelDao().loadAllChannelsByTimeAndTagSync(currentTime, channelTagIds, sortOrder);
            }
        }
    }

    private static class ItemSubsetsLoaderTask extends AsyncTask<Void, Void, List<EpgChannel>> {
        private final AppRoomDatabase db;
        private final int sortOrder;
        private final Set<Integer> channelTagIds;

        ItemSubsetsLoaderTask(AppRoomDatabase db, Set<Integer> channelTagIds, int sortOrder) {
            this.db = db;
            this.channelTagIds = channelTagIds;
            this.sortOrder = sortOrder;
        }

        @Override
        protected List<EpgChannel> doInBackground(Void... voids) {
            if (channelTagIds.size() == 0) {
                return db.getChannelDao().loadAllChannelsNamesOnlySync(sortOrder);
            } else {
                return db.getChannelDao().loadAllChannelsNamesOnlyByTagSync(channelTagIds, sortOrder);
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
