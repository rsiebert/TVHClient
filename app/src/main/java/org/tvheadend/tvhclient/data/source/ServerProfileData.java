package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

public class ServerProfileData implements DataSourceInterface<ServerProfile> {

    private final static int RECORDINGS = 1;
    private final static int HTSP_PLAYBACK = 2;
    private final static int HTTP_PLAYBACK = 3;
    private final AppRoomDatabase db;

    @Inject
    public ServerProfileData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerProfile item) {
        db.getServerProfileDao().insert(item);
    }

    @Override
    public void updateItem(ServerProfile item) {
        db.getServerProfileDao().update(item);
    }

    @Override
    public void removeItem(ServerProfile item) {
        db.getServerProfileDao().delete(item);
    }

    public void removeAll() {
        db.getServerProfileDao().deleteAll();
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
            return new ItemLoaderTask(db, id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<ServerProfile> getItems() {
        return new ArrayList<>();
    }

    public String[] getRecordingProfileNames() {
        List<ServerProfile> serverProfiles = getRecordingProfiles();
        if (serverProfiles != null) {
            String[] names = new String[serverProfiles.size()];
            for (int i = 0; i < serverProfiles.size(); i++) {
                names[i] = serverProfiles.get(i).getName();
            }
            return names;
        }
        return null;
    }

    public String[] getHtspPlaybackProfileNames() {
        List<ServerProfile> serverProfiles = getHtspPlaybackProfiles();
        if (serverProfiles != null) {
            String[] names = new String[serverProfiles.size()];
            for (int i = 0; i < serverProfiles.size(); i++) {
                names[i] = serverProfiles.get(i).getName();
            }
            return names;
        }
        return null;
    }

    public String[] getHttpPlaybackProfileNames() {
        List<ServerProfile> serverProfiles = getHttpPlaybackProfiles();
        if (serverProfiles != null) {
            String[] names = new String[serverProfiles.size()];
            for (int i = 0; i < serverProfiles.size(); i++) {
                names[i] = serverProfiles.get(i).getName();
            }
            return names;
        }
        return null;
    }

    public List<ServerProfile> getRecordingProfiles() {
        try {
            return new ItemsLoaderTask(db, RECORDINGS).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getHtspPlaybackProfiles() {
        try {
            return new ItemsLoaderTask(db, HTSP_PLAYBACK).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getHttpPlaybackProfiles() {
        try {
            return new ItemsLoaderTask(db, HTTP_PLAYBACK).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, ServerProfile> {
        private final AppRoomDatabase db;
        private final Object id;

        ItemLoaderTask(AppRoomDatabase db, Object id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ServerProfile doInBackground(Void... voids) {
            if (id instanceof Integer) {
                return db.getServerProfileDao().loadProfileByIdSync((int) id);
            } else if (id instanceof String) {
                return db.getServerProfileDao().loadProfileByUuidSync((String) id);
            }
            return null;
        }
    }

    private static class ItemsLoaderTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final AppRoomDatabase db;
        private final int type;

        ItemsLoaderTask(AppRoomDatabase db, int type) {
            this.db = db;
            this.type = type;
        }

        @Override
        protected List<ServerProfile> doInBackground(Void... voids) {
            switch (type) {
                case HTSP_PLAYBACK:
                    return db.getServerProfileDao().loadHtspPlaybackProfilesSync();
                case HTTP_PLAYBACK:
                    return db.getServerProfileDao().loadHttpPlaybackProfilesSync();
                case RECORDINGS:
                    return db.getServerProfileDao().loadAllRecordingProfilesSync();
                default:
                    return null;
            }
        }
    }
}
