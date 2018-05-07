package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ChannelTagData implements DataSourceInterface<ChannelTag> {

    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;
    private AppRoomDatabase db;

    @Inject
    public ChannelTagData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ChannelTag item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void addItems(List<ChannelTag> items) {
        for (ChannelTag channel : items) {
            addItem(channel);
        }
    }

    @Override
    public void updateItem(ChannelTag item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void updateItems(List<ChannelTag> items) {
        for (ChannelTag channel : items) {
            updateItem(channel);
        }
    }

    @Override
    public void removeItem(ChannelTag item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public void removeItems(List<ChannelTag> items) {
        for (ChannelTag channel : items) {
            removeItem(channel);
        }
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    public LiveData<List<ChannelTag>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<ChannelTag> getLiveDataItemById(Object id) {
        return null;
    }

    @Override
    public ChannelTag getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ChannelTag> getItems() {
        try {
            return new ItemsLoaderTask(db).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static class ItemLoaderTask extends AsyncTask<Void, Void, ChannelTag> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ChannelTag doInBackground(Void... voids) {
            return db.getChannelTagDao().loadChannelTagByIdSync(id);
        }
    }

    protected static class ItemsLoaderTask extends AsyncTask<Void, Void, List<ChannelTag>> {
        private final AppRoomDatabase db;

        ItemsLoaderTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<ChannelTag> doInBackground(Void... voids) {
            return db.getChannelTagDao().loadAllChannelTagsSync();
        }
    }

    protected static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final ChannelTag channelTag;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, ChannelTag channelTag, int type) {
            this.db = db;
            this.channelTag = channelTag;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getChannelTagDao().insert(channelTag);
                    break;
                case UPDATE:
                    db.getChannelTagDao().update(channelTag);
                    break;
                case DELETE:
                    db.getChannelTagDao().delete(channelTag);
                    break;
            }
            return null;
        }
    }
}
