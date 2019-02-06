package org.tvheadend.tvhclient.data.source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
    public LiveData<TagAndChannel> getLiveDataItemById(Object id) {
        return null;
    }

    @Override
    public TagAndChannel getItemById(Object id) {
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
