package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;
import org.tvheadend.tvhclient.data.local.dao.ServerProfileDao;
import org.tvheadend.tvhclient.data.local.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.local.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConfigRepository {
    private final AppRoomDatabase db;

    public ConfigRepository(Context context) {
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
    }

    public TranscodingProfile getPlaybackTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.getTranscodingProfileDao(), db.getServerStatusDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TranscodingProfile getRecordingTranscodingProfile() {
        try {
            return new LoadTranscodingProfileTask(db.getTranscodingProfileDao(), db.getServerStatusDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getPlaybackServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.getServerProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getRecordingServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.getServerProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServerProfile getCastingServerProfileById(int id) {
        try {
            return new LoadServerProfileByIdTask(db.getServerProfileDao(), id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllPlaybackServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.getServerProfileDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerProfile> getAllRecordingServerProfiles() {
        try {
            return new LoadAllServerProfilesTask(db.getServerProfileDao(), "recording").execute().get();
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

    public void updatePlaybackServerProfile(int id) {
        new UpdateServerProfileTask(db.getServerStatusDao(), "playback", id).execute();
    }

    public void updateRecordingServerProfile(int id) {
        new UpdateServerProfileTask(db.getServerStatusDao(), "recording", id).execute();
    }

    public void updateCastingServerProfile(int id) {
        new UpdateServerProfileTask(db.getServerStatusDao(), "casting", id).execute();
    }

    public ServerStatus getServerStatus() {
        try {
            return new LoadServerStatusTask(db.getServerStatusDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class LoadServerStatusTask extends AsyncTask<Void, Void, ServerStatus> {
        private final ServerStatusDao dao;

        LoadServerStatusTask(ServerStatusDao dao) {
            this.dao = dao;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {
            return dao.loadServerStatusSync();
        }
    }

    protected static class LoadTranscodingProfileTask extends AsyncTask<Void, Void, TranscodingProfile> {
        private final TranscodingProfileDao transcodingProfileDao;
        private final ServerStatusDao serverStatusDao;
        private final String type;

        LoadTranscodingProfileTask(TranscodingProfileDao transcodingProfileDao, ServerStatusDao serverStatusDao, String type) {
            this.transcodingProfileDao = transcodingProfileDao;
            this.serverStatusDao = serverStatusDao;
            this.type = type;
        }

        @Override
        protected TranscodingProfile doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            TranscodingProfile profile = null;
            switch (type) {
                case "playback":
                    profile = transcodingProfileDao.loadProfileByIdSync(serverStatus.getPlaybackTranscodingProfileId());
                    break;
                case "recording":
                    profile = transcodingProfileDao.loadProfileByIdSync(serverStatus.getRecordingTranscodingProfileId());
                    break;
            }
            if (profile != null) {
                return profile;
            } else {
                return new TranscodingProfile();
            }
        }
    }

    protected static class LoadServerProfileByIdTask extends AsyncTask<Void, Void, ServerProfile> {
        private final ServerProfileDao serverProfileDao;
        private final int id;

        LoadServerProfileByIdTask(ServerProfileDao serverProfileDao, int id) {
            this.serverProfileDao = serverProfileDao;
            this.id = id;
        }

        @Override
        protected ServerProfile doInBackground(Void... voids) {
            ServerProfile profile = serverProfileDao.loadProfileByIdSync(id);
            if (profile != null) {
                return profile;
            } else {
                return new ServerProfile();
            }
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
            List<ServerProfile> profiles = null;
            switch (type) {
                case "playback":
                    profiles = dao.loadAllPlaybackProfilesSync();
                    break;
                case "recording":
                    profiles = dao.loadAllRecordingProfilesSync();
                    break;
            }
            if (profiles != null) {
                return profiles;
            } else {
                return new ArrayList<>();
            }
        }
    }

    protected static class UpdateServerProfileTask extends AsyncTask<Void, Void, Void> {
        private final String type;
        private final int id;
        private final ServerStatusDao serverStatusDao;

        UpdateServerProfileTask(ServerStatusDao serverStatusDao, String type, int id) {
            this.serverStatusDao = serverStatusDao;
            this.type = type;
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            Log.d("Update", "doInBackground: updating profile " + type + ", id " + id);
            switch (type) {
                case "playback":
                    serverStatus.setPlaybackServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
                case "recording":
                    serverStatus.setRecordingServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
                case "casting":
                    serverStatus.setCastingServerProfileId(id);
                    serverStatusDao.update(serverStatus);
                    break;
            }
            return null;
        }
    }

    public void updatePlaybackTranscodingProfile(TranscodingProfile playbackProfile) {
        new UpdateTranscodingProfileTask(db.getTranscodingProfileDao(), db.getServerStatusDao(), "playback", playbackProfile).execute();
    }

    public void updateRecordingTranscodingProfile(TranscodingProfile recordingProfile) {
        new UpdateTranscodingProfileTask(db.getTranscodingProfileDao(), db.getServerStatusDao(), "recording", recordingProfile).execute();
    }

    protected static class UpdateTranscodingProfileTask extends AsyncTask<Void, Void, Void> {
        private final TranscodingProfileDao transcodingProfileDao;
        private final String type;
        private final TranscodingProfile profile;
        private final ServerStatusDao serverStatusDao;

        UpdateTranscodingProfileTask(TranscodingProfileDao transcodingProfileDao, ServerStatusDao serverStatusDao, String type, TranscodingProfile profile) {
            this.transcodingProfileDao = transcodingProfileDao;
            this.serverStatusDao = serverStatusDao;
            this.type = type;
            this.profile = profile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = serverStatusDao.loadServerStatusSync();
            switch (type) {
                case "playback":
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        serverStatus.setPlaybackTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        serverStatus.setPlaybackTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
                case "recording":
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        serverStatus.setRecordingTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        serverStatus.setRecordingTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
            }
            return null;
        }
    }
/*
    public ChannelTag getSelectedChannelTag() {
        try {
            return new LoadSelectedChannelTagTask(db.getServerStatusDao(), db.getChannelTagDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class LoadSelectedChannelTagTask extends AsyncTask<Void, Void, ChannelTag> {
        private final ServerStatusDao getServerStatusDao;
        private final ChannelTagDao getChannelTagDao;

        LoadSelectedChannelTagTask(ServerStatusDao getServerStatusDao, ChannelTagDao getChannelTagDao) {
            this.getServerStatusDao = getServerStatusDao;
            this.getChannelTagDao = getChannelTagDao;
        }

        @Override
        protected ChannelTag doInBackground(Void... voids) {
            ServerStatus serverStatus = getServerStatusDao.loadServerStatusSync();
            return getChannelTagDao.loadChannelTagByIdSync(serverStatus.getChannelTagId());
        }
    }

    public void setSelectedChannelTag(int id) {
        new UpdateChannelTagTask(db.getServerStatusDao(), id).execute();
    }

    private static class UpdateChannelTagTask extends AsyncTask<Void, Void, Void> {
        private final ServerStatusDao dao;
        private final int id;

        UpdateChannelTagTask(ServerStatusDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ServerStatus serverStatus = dao.loadServerStatusSync();
            serverStatus.setChannelTagId(id);
            dao.update(serverStatus);
            return null;
        }
    }
*/
}
