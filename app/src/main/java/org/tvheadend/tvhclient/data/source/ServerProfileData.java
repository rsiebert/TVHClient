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

public class ServerProfileData extends BaseData implements DataSourceInterface<ServerProfile> {

    private final static int RECORDINGS = 1;
    private final static int PROGRAMS = 2;
    private final AppRoomDatabase db;

    @Inject
    public ServerProfileData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerProfile item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void updateItem(ServerProfile item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(ServerProfile item) {
        new ItemHandlerTask(db, item, DELETE).execute();
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

    public String[] getPlaybackProfileNames() {
        List<ServerProfile> serverProfiles = getPlaybackProfiles();
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

    public List<ServerProfile> getPlaybackProfiles() {
        try {
            return new ItemsLoaderTask(db, PROGRAMS).execute().get();
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

    protected static class ItemsLoaderTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final AppRoomDatabase db;
        private final int type;

        ItemsLoaderTask(AppRoomDatabase db, int type) {
            this.db = db;
            this.type = type;
        }

        @Override
        protected List<ServerProfile> doInBackground(Void... voids) {
            switch (type) {
                case PROGRAMS:
                    return db.getServerProfileDao().loadAllPlaybackProfilesSync();
                case RECORDINGS:
                    return db.getServerProfileDao().loadAllRecordingProfilesSync();
                default:
                    return null;
            }
        }
    }

    protected static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final ServerProfile serverProfile;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, ServerProfile serverProfile, int type) {
            this.db = db;
            this.serverProfile = serverProfile;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getServerProfileDao().insert(serverProfile);
                    break;
                case UPDATE:
                    db.getServerProfileDao().update(serverProfile);
                    break;
                case DELETE:
                    db.getServerProfileDao().delete(serverProfile);
                    break;
            }
            return null;
        }
    }
}
