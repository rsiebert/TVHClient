package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.ChannelTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import timber.log.Timber;

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
    @Nullable
    public LiveData<ChannelTag> getLiveDataItemById(Object id) {
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    @Nullable
    public ChannelTag getItemById(Object id) {
        try {
            return new ChannelTagByIdTask(db, (int) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel tag by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel tag by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<ChannelTag> getItems() {
        List<ChannelTag> channelTags = new ArrayList<>();
        try {
            channelTags = new ChannelTagListTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading all channel tags task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading all channel tags task aborted", e);
        }
        return channelTags;
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
            tags = new SelectedChannelTagIdListTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading selected channel tag ids task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading selected channel tag ids task aborted", e);
        }
        return new HashSet<>(tags);
    }

    public int getItemCount() {
        try {
            return new ChannelTagCountTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading channel tag count task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading channel tag count task aborted", e);
        }
        return 0;
    }

    private static class SelectedChannelTagIdListTask extends AsyncTask<Void, Void, List<Integer>> {
        private final AppRoomDatabase db;

        SelectedChannelTagIdListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Integer> doInBackground(Void... voids) {
            return db.getChannelTagDao().loadAllSelectedChannelTagIds();
        }
    }

    private static class ChannelTagListTask extends AsyncTask<Void, Void, List<ChannelTag>> {
        private final AppRoomDatabase db;

        ChannelTagListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<ChannelTag> doInBackground(Void... voids) {
            return db.getChannelTagDao().loadAllChannelTagsSync();
        }
    }

    private static class ChannelTagCountTask extends AsyncTask<Void, Void, Integer> {
        private final AppRoomDatabase db;

        ChannelTagCountTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return db.getChannelTagDao().getItemCountSync();
        }
    }

    private static class ChannelTagByIdTask extends AsyncTask<Void, Void, ChannelTag> {
        private final AppRoomDatabase db;
        private final int id;

        ChannelTagByIdTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ChannelTag doInBackground(Void... voids) {
            return db.getChannelTagDao().loadChannelTagByIdSync(id);
        }
    }
}
