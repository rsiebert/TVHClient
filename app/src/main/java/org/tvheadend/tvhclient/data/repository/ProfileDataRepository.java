package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ProfileDataRepository {

    private final AppDatabase db;

    public ProfileDataRepository(Context context) {
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

    public TranscodingProfile getPlaybackTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.transcodingProfileDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TranscodingProfile getRecordingTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.transcodingProfileDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updatePlaybackTranscodingProfile(TranscodingProfile profile) {
        new UpdateProfileTask(db.serverStatusDao(), "recording", null, profile).execute();
    }

    public void updateRecordingTranscodingProfile(TranscodingProfile profile) {
        new UpdateProfileTask(db.serverStatusDao(), "playback", null, profile).execute();
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

    protected static class UpdateProfileTask extends AsyncTask<Void, Void, Void> {
        private final ServerStatusDao serverDao;
        private final String type;
        private final ServerProfile serverProfile;
        private final TranscodingProfile transcodingProfile;

        UpdateProfileTask(ServerStatusDao serverDao, String type, ServerProfile serverProfile, TranscodingProfile transcodingProfile) {
            this.serverDao = serverDao;
            this.type = type;
            this.serverProfile = serverProfile;
            this.transcodingProfile = transcodingProfile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = serverDao.loadServerStatusSync();
            switch (type) {
                case "playback_server_profile":
                    serverStatus.setPlaybackServerProfileId(serverProfile.getId());
                    serverDao.update(serverStatus);
                case "recording_server_profile":
                    serverStatus.setPlaybackServerProfileId(serverProfile.getId());
                    serverDao.update(serverStatus);
                case "casting_server_profile":
                    serverStatus.setPlaybackServerProfileId(serverProfile.getId());
                    serverDao.update(serverStatus);
                case "playback_transcoding_profile":
                    serverStatus.setPlaybackTranscodingProfileId(transcodingProfile.getId());
                    serverDao.update(serverStatus);
                case "recording_transcoding_profile":
                    serverStatus.setRecordingTranscodingProfileId(transcodingProfile.getId());
                    serverDao.update(serverStatus);
            }
            return null;
        }
    }

    protected static class LoadTranscodingProfileTask extends AsyncTask<Void, Void, TranscodingProfile> {
        private final TranscodingProfileDao dao;
        private final String type;

        LoadTranscodingProfileTask(TranscodingProfileDao dao, String type) {
            this.dao = dao;
            this.type = type;
        }

        @Override
        protected TranscodingProfile doInBackground(Void... voids) {
            switch (type) {
                case "playback":
                    return dao.loadPlaybackProfileSync();
                case "recording":
                    return dao.loadRecordingProfileSync();
            }
            return null;
        }
    }
}
