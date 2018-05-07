package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ServerProfileData implements DataSourceInterface<ServerProfile> {

    private final AppRoomDatabase db;
    private String[] recordingProfiles;

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

    public String[] getRecordingProfileNames() {
        try {
            List<ServerProfile> serverProfiles = new ItemsLoaderTask(db, "recording").execute().get();
            if (serverProfiles != null) {
                String[] names = new String[serverProfiles.size()];
                for (int i = 0; i < serverProfiles.size(); i++) {
                    names[i] = serverProfiles.get(i).getName();
                }
                return names;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getPlaybackProfileNames() {
        try {
            List<ServerProfile> serverProfiles = new ItemsLoaderTask(db, "playback").execute().get();
            if (serverProfiles != null) {
                String[] names = new String[serverProfiles.size()];
                for (int i = 0; i < serverProfiles.size(); i++) {
                    names[i] = serverProfiles.get(i).getName();
                }
                return names;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ServerProfile> getItems() {
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

    protected static class ItemsLoaderTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final AppRoomDatabase db;
        private final String type;

        ItemsLoaderTask(AppRoomDatabase db, String type) {
            this.db = db;
            this.type = type;
        }

        @Override
        protected List<ServerProfile> doInBackground(Void... voids) {
            switch (type) {
                case "playback":
                    return db.getServerProfileDao().loadAllPlaybackProfilesSync();
                case "recording":
                    return db.getServerProfileDao().loadAllRecordingProfilesSync();
                default:
                    return null;
            }
        }
    }
}
