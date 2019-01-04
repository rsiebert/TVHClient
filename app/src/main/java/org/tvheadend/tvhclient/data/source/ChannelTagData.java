package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.support.annotation.WorkerThread;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.ChannelTag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class ChannelTagData implements DataSourceInterface<ChannelTag> {

    private final AppRoomDatabase db;

    @Inject
    public ChannelTagData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ChannelTag item) {
        new Thread(() -> db.getChannelTagDao().insert(item)).start();
    }

    @Override
    public void updateItem(ChannelTag item) {
        new Thread(() -> db.getChannelTagDao().update(item)).start();
    }

    @Override
    public void removeItem(ChannelTag item) {
        new Thread(() -> db.getChannelTagDao().delete(item)).start();
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
     *
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
        List<Integer> tags = db.getChannelTagDao().loadAllSelectedChannelTagIds();
        return new HashSet<>(tags);
    }

    /**
     * @param ids
     */
    public void updateSelectedChannelTags(Set<Integer> ids) {
        new Thread(() -> {
            List<ChannelTag> channelTags = db.getChannelTagDao().loadAllChannelTagsSync();
            for (ChannelTag channelTag : channelTags) {
                channelTag.setIsSelected(0);
                if (ids.contains(channelTag.getTagId())) {
                    channelTag.setIsSelected(1);
                }
            }
            db.getChannelTagDao().update(channelTags);
        }).start();
    }
}
