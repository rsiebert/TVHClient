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
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelData implements DataSourceInterface<Channel> {

    private final AppRoomDatabase db;
    private final Context context;

    @Inject
    public ChannelData(AppRoomDatabase database, Context context) {
        this.db = database;
        this.context = context;
    }

    @Override
    public void addItem(Channel item) {
        db.getChannelDao().insert(item);
    }

    public void addItems(@NonNull List<Channel> items) {
        db.getChannelDao().insert(items);
    }

    @Override
    public void updateItem(Channel item) {
        db.getChannelDao().update(item);
    }

    @Override
    public void removeItem(Channel item) {
        db.getChannelDao().delete(item);
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

    public Channel getItemByIdWithPrograms(int id, long selectedTime) {
        try {
            return new ItemLoaderTask(db, id, selectedTime).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param channelSortOrder
     * @param tagIds
     * @return
     */
    public LiveData<List<EpgChannel>> getAllEpgChannels(int channelSortOrder, @NonNull Set<Integer> tagIds) {
        if (tagIds.size() == 0) {
            return db.getChannelDao().loadAllEpgChannels(channelSortOrder);
        } else {
            return db.getChannelDao().loadAllEpgChannelsByTag(channelSortOrder, tagIds);
        }
    }

    /**
     *
     * @param selectedTime
     * @param channelSortOrder
     * @param tagIds
     * @return
     */
    public LiveData<List<Channel>> getAllChannelsByTime(long selectedTime, int channelSortOrder, @NonNull Set<Integer> tagIds) {
        if (tagIds.size() == 0) {
            return db.getChannelDao().loadAllChannelsByTime(selectedTime, channelSortOrder);
        } else {
            return db.getChannelDao().loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds);
        }
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
}
