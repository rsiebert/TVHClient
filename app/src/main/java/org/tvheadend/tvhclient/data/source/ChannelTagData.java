package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.WorkerThread;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.ChannelTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelTagData implements DataSourceInterface<ChannelTag> {

    private final AppRoomDatabase db;

    @Inject
    public ChannelTagData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().insert(item));
    }

    @Override
    public void updateItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().update(item));
    }

    @Override
    public void removeItem(ChannelTag item) {
        AsyncTask.execute(() -> db.getChannelTagDao().delete(item));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public LiveData<List<ChannelTag>> getLiveDataItems() {
        return db.getChannelTagDao().loadAllChannelTags();
    }

    @Override
    public LiveData<ChannelTag> getLiveDataItemById(Object id) {
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    @WorkerThread
    public ChannelTag getItemById(Object id) {
        return db.getChannelTagDao().loadChannelTagByIdSync((int) id);
    }

    @Override
    @WorkerThread
    public List<ChannelTag> getItems() {
        return null;
    }

    /**
     * Returns the ids of the channel tags that are marked as selected.
     * This method must not be called from the UI thread.
     *
     * @return
     */
    @WorkerThread
    public Set<Integer> getSelectedChannelTagIds() {
        List<Integer> tags = new ArrayList<>();
        try {
            tags = new ItemsLoaderTask(db).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new HashSet<>(tags);
    }

    /**
     * @param ids
     */
    public void updateSelectedChannelTags(Set<Integer> ids) {
        AsyncTask.execute(() -> {
            List<ChannelTag> channelTags = db.getChannelTagDao().loadAllChannelTagsSync();
            for (ChannelTag channelTag : channelTags) {
                channelTag.setIsSelected(0);
                if (ids.contains(channelTag.getTagId())) {
                    channelTag.setIsSelected(1);
                }
            }
            db.getChannelTagDao().update(channelTags);
        });
    }

    private static class ItemsLoaderTask extends AsyncTask<Void, Void, List<Integer>> {
        private final AppRoomDatabase db;

        ItemsLoaderTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Integer> doInBackground(Void... voids) {
            return db.getChannelTagDao().loadAllSelectedChannelTagIds();
        }
    }
}
