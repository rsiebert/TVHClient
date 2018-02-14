package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.entity.ServerProfile;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ProfileRepository {

    private final AppDatabase db;

    public ProfileRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public ServerProfile getPlaybackServerProfile() {
        // Get the playback serverProfile of the currently active connection
        try {
            return new LoadServerProfileTask(db.serverProfileDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new ServerProfile();
    }

    public ServerProfile getRecordingServerProfile() {
        // Get the recording serverProfile of the currently active connection
        try {
            return new LoadServerProfileTask(db.serverProfileDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getCastingServerProfile() {
        // Get the casting serverProfile of the currently active connection
        try {
            return new LoadServerProfileTask(db.serverProfileDao(), "casting").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllRecordingServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.serverProfileDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllPlaybackServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.serverProfileDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getAllRecordingServerProfileNames() {
        List<ServerProfile> profiles = getAllRecordingServerProfiles();
        String[] recordingProfilesList = new String[profiles.size()];
        for (int i = 0; i < recordingProfilesList.length; i++) {
            recordingProfilesList[i] = profiles.get(i).getName();
        }
        return recordingProfilesList;
    }

    protected static class LoadServerProfileTask extends AsyncTask<Void, Void, ServerProfile> {
        private final ServerProfileDao dao;
        private final String type;

        LoadServerProfileTask(ServerProfileDao dao, String type) {
            this.dao = dao;
            this.type = type;
        }

        @Override
        protected ServerProfile doInBackground(Void... voids) {
            switch (type) {
                case "playback":
                    return dao.loadPlaybackProfileSync();
                case "recording":
                    return dao.loadRecordingProfileSync();
                case "casting":
                    return dao.loadCastingProfileSync();
            }
            return null;
        }
    }

    protected static class LoadAllServerProfilesTask extends AsyncTask<Void, Void, List<ServerProfile>> {
        private final ServerProfileDao dao;
        private final String type;

        LoadAllServerProfilesTask(ServerProfileDao dao, String type) {
            this.dao = dao;
            this.type = type;
        }

        @Override
        protected List<ServerProfile> doInBackground(Void... voids) {
            switch (type) {
                case "playback":
                    return dao.loadAllPlaybackProfilesSync();
                case "recording":
                    return dao.loadAllRecordingProfilesSync();
            }
            return null;
        }
    }

}
