package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ServerProfileData implements DataSourceInterface<ServerProfile> {

    private final AppRoomDatabase db;

    @Inject
    public ServerProfileData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerProfile item) {

    }

    @Override
    public void addItems(List<ServerProfile> items) {

    }

    @Override
    public void updateItem(ServerProfile item) {

    }

    @Override
    public void updateItems(List<ServerProfile> items) {

    }

    @Override
    public void removeItem(ServerProfile item) {

    }

    @Override
    public void removeItems(List<ServerProfile> items) {

    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    public LiveData<List<ServerProfile>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<ServerProfile> getLiveDataItemById(Object id) {
        return null;
    }

    @Override
    public ServerProfile getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, ServerProfile> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ServerProfile doInBackground(Void... voids) {
            return db.getServerProfileDao().loadProfileByIdSync(id);
        }
    }
}
