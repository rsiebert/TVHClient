package org.tvheadend.tvhclient.data.source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.db.AppRoomDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import timber.log.Timber;

public class ServerProfileData implements DataSourceInterface<ServerProfile> {

    private final static int RECORDINGS = 1;
    private final static int HTSP_PLAYBACK = 2;
    private final static int HTTP_PLAYBACK = 3;
    private final AppRoomDatabase db;

    public ServerProfileData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerProfile item) {
        AsyncTask.execute(() -> db.getServerProfileDao().insert(item));
    }

    @Override
    public void updateItem(ServerProfile item) {
        AsyncTask.execute(() -> db.getServerProfileDao().update(item));
    }

    @Override
    public void removeItem(ServerProfile item) {
        AsyncTask.execute(() -> db.getServerProfileDao().delete(item));
    }

    public void removeAll() {
        AsyncTask.execute(() -> db.getServerProfileDao().deleteAll());
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
    @Nullable
    public LiveData<ServerProfile> getLiveDataItemById(Object id) {
        return null;
    }

    @Override
    @Nullable
    public ServerProfile getItemById(@NonNull Object id) {
        try {
            return new ServerProfileByIdTask(db, id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading server profile by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading server profile by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<ServerProfile> getItems() {
        return new ArrayList<>();
    }

    @NonNull
    public String[] getRecordingProfileNames() {
        return getProfileNames(getRecordingProfiles());
    }

    @NonNull
    public String[] getHtspPlaybackProfileNames() {
        return getProfileNames(getHtspPlaybackProfiles());
    }

    @NonNull
    public String[] getHttpPlaybackProfileNames() {
        return getProfileNames(getHttpPlaybackProfiles());
    }

    @NonNull
    private String[] getProfileNames(@NonNull List<ServerProfile> serverProfiles) {
        if (serverProfiles.size() > 0) {
            String[] names = new String[serverProfiles.size()];
            for (int i = 0; i < serverProfiles.size(); i++) {
                names[i] = serverProfiles.get(i).getName();
            }
            return names;
        }
        return new String[0];
    }

    @NonNull
    public List<ServerProfile> getRecordingProfiles() {
        List<ServerProfile> serverProfiles = new ArrayList<>();
        try {
            serverProfiles.addAll(new ServerProfileTask(db, RECORDINGS).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading recording server profile task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading recording server profile task aborted", e);
        }
        return serverProfiles;
    }

    @NonNull
    public List<ServerProfile> getHtspPlaybackProfiles() {
        List<ServerProfile> serverProfiles = new ArrayList<>();
        try {
            serverProfiles.addAll(new ServerProfileTask(db, HTSP_PLAYBACK).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading htsp playback server profile task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading htsp playback server profile task aborted", e);
        }
        return serverProfiles;
    }

    @NonNull
    public List<ServerProfile> getHttpPlaybackProfiles() {
        List<ServerProfile> serverProfiles = new ArrayList<>();
        try {
            serverProfiles.addAll(new ServerProfileTask(db, HTTP_PLAYBACK).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading http playback server profile task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading http playback server profile task aborted", e);
        }
        return serverProfiles;
    }

    private static class ServerProfileByIdTask extends AsyncTask<Void, Void, ServerProfile> {
        private final AppRoomDatabase db;
        private final Object id;

        ServerProfileByIdTask(AppRoomDatabase db, Object id) {
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

    private static class ServerProfileTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final AppRoomDatabase db;
        private final int type;

        ServerProfileTask(AppRoomDatabase db, int type) {
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
