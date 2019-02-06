package org.tvheadend.tvhclient.data.source;

import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.EpgChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import timber.log.Timber;

public class ChannelData implements DataSourceInterface<Channel> {

    private final AppRoomDatabase db;

    public ChannelData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Channel item) {
        AsyncTask.execute(() -> db.getChannelDao().insert(item));
    }

    public void addItems(@NonNull List<Channel> items) {
        AsyncTask.execute(() -> db.getChannelDao().insert(new ArrayList<>(items)));
    }

    @Override
    public void updateItem(Channel item) {
        AsyncTask.execute(() -> db.getChannelDao().update(item));
    }

    @Override
    public void removeItem(Channel item) {
        AsyncTask.execute(() -> db.getChannelDao().delete(item));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getChannelDao().getItemCount();
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
    @Nullable
    public Channel getItemById(Object id) {
        try {
            return new ChannelByIdTask(db, (int) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<Channel> getItems() {
        List<Channel> channels = new ArrayList<>();
        try {
            channels.addAll(new ChannelListTask(db).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading all channels task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading all channels task aborted", e);
        }
        return channels;
    }

    public int getItemCount() {
        try {
            return new ChannelCountTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel count task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel count task aborted", e);
        }
        return 0;
    }

    @Nullable
    public Channel getItemByIdWithPrograms(int id, long selectedTime) {
        try {
            return new ChannelByIdTask(db, id, selectedTime).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel by id task aborted", e);
        }
        return null;
    }

    public LiveData<List<EpgChannel>> getAllEpgChannels(int channelSortOrder, @NonNull List<Integer> tagIds) {
        Timber.d("Loading epg channels with sort order " + channelSortOrder + " and " + tagIds.size() + " tags");
        if (tagIds.size() == 0) {
            return db.getChannelDao().loadAllEpgChannels(channelSortOrder);
        } else {
            return db.getChannelDao().loadAllEpgChannelsByTag(channelSortOrder, tagIds);
        }
    }

    public LiveData<List<Channel>> getAllChannelsByTime(long selectedTime, int channelSortOrder, @NonNull List<Integer> tagIds) {
        Timber.d("Loading channels from time " + selectedTime + " with sort order " + channelSortOrder + " and " + tagIds.size() + " tags");
        if (tagIds.size() == 0) {
            return db.getChannelDao().loadAllChannelsByTime(selectedTime, channelSortOrder);
        } else {
            return db.getChannelDao().loadAllChannelsByTimeAndTag(selectedTime, channelSortOrder, tagIds);
        }
    }

    private static class ChannelByIdTask extends AsyncTask<Void, Void, Channel> {
        private final AppRoomDatabase db;
        private final int id;
        private final long selectedTime;

        ChannelByIdTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
            this.selectedTime = 0;
        }

        ChannelByIdTask(AppRoomDatabase db, int id, long selectedTime) {
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

    private static class ChannelListTask extends AsyncTask<Void, Void, List<Channel>> {
        private final AppRoomDatabase db;

        ChannelListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {
            return db.getChannelDao().loadAllChannelsSync(0);
        }
    }

    private static class ChannelCountTask extends AsyncTask<Void, Void, Integer> {
        private final AppRoomDatabase db;

        ChannelCountTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return db.getChannelDao().getItemCountSync();
        }
    }
}
