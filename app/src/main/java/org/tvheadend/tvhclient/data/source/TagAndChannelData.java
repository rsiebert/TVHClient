package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TagAndChannelData extends BaseData implements DataSourceInterface<TagAndChannel> {

    private final AppRoomDatabase db;

    @Inject
    public TagAndChannelData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(TagAndChannel item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void updateItem(TagAndChannel item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(TagAndChannel item) {
        new ItemHandlerTask(db, item, DELETE).execute();
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

    public void removeItemByTagId(int tagId) {
        new ItemMiscTask(db, DELETE_BY_ID, tagId).execute();
    }

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final TagAndChannel tagAndChannel;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, TagAndChannel tagAndChannel, int type) {
            this.db = db;
            this.tagAndChannel = tagAndChannel;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getTagAndChannelDao().insert(tagAndChannel);
                    break;
                case UPDATE:
                    db.getTagAndChannelDao().update(tagAndChannel);
                    break;
                case DELETE:
                    db.getTagAndChannelDao().delete(tagAndChannel);
                    break;
            }
            return null;
        }
    }

    private static class ItemMiscTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final int type;
        private final Object arg;

        ItemMiscTask(AppRoomDatabase db, int type, Object arg) {
            this.db = db;
            this.type = type;
            this.arg = arg;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case DELETE_BY_ID:
                    db.getTagAndChannelDao().deleteByTagId((int) arg);
                    break;
            }
            return null;
        }
    }
}
