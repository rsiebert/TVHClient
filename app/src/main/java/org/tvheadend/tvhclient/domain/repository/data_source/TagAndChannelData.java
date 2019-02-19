package org.tvheadend.tvhclient.domain.repository.data_source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.TagAndChannel;

import java.util.ArrayList;
import java.util.List;

public class TagAndChannelData implements DataSourceInterface<TagAndChannel> {

    private final AppRoomDatabase db;

    public TagAndChannelData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(TagAndChannel item) {
        AsyncTask.execute(() -> db.getTagAndChannelDao().insert(item));
    }

    @Override
    public void updateItem(TagAndChannel item) {
        AsyncTask.execute(() -> db.getTagAndChannelDao().update(item));
    }

    @Override
    public void removeItem(TagAndChannel item) {
        AsyncTask.execute(() -> db.getTagAndChannelDao().delete(item));
    }

    public void addAndRemoveItems(List<TagAndChannel> newItems, List<TagAndChannel> oldItems) {
        AsyncTask.execute(() -> db.getTagAndChannelDao().insertAndDelete(
                new ArrayList<>(newItems),
                new ArrayList<>(oldItems)));
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    public LiveData<List<TagAndChannel>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<TagAndChannel> getLiveDataItemById(@NonNull Object id) {
        return null;
    }

    @Override
    public TagAndChannel getItemById(@NonNull Object id) {
        return null;
    }

    @Override
    @NonNull
    public List<TagAndChannel> getItems() {
        return new ArrayList<>();
    }

    public void removeItemByTagId(int id) {
        AsyncTask.execute(() -> db.getTagAndChannelDao().deleteByTagId(id));
    }
}
