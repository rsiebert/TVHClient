package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;

import java.util.concurrent.ExecutionException;

public class ConfigRepository {
    private String TAG = getClass().getSimpleName();
    private final AppDatabase db;

    public ConfigRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public TranscodingProfile getPlaybackTranscodingProfile() {
        TranscodingProfile profile = null;
        try {
            profile = new LoadTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "playback").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (profile == null) {
            Log.d(TAG, "getPlaybackTranscodingProfile: loaded profile with id 0");
            return new TranscodingProfile();
        } else {
            Log.d(TAG, "getPlaybackTranscodingProfile: loaded profile with id " + profile.getId());
            return profile;
        }
    }

    public TranscodingProfile getRecordingTranscodingProfile() {
        TranscodingProfile profile = null;
        try {
            profile = new LoadTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "recording").execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (profile == null) {
            Log.d(TAG, "getRecordingTranscodingProfile: loaded profile with id 0");
            return new TranscodingProfile();
        } else {
            Log.d(TAG, "getRecordingTranscodingProfile: loaded profile with id " + profile.getId());
            return profile;
        }
    }

    protected static class LoadTranscodingProfileTask extends AsyncTask<Void, Void, TranscodingProfile> {
        private String TAG = getClass().getSimpleName();
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
            switch (type) {
                case "playback":
                    Log.d(TAG, "doInBackground: loading playback profile");
                    return transcodingProfileDao.loadProfileByIdSync(serverStatus.getPlaybackTranscodingProfileId());
                case "recording":
                    Log.d(TAG, "doInBackground: loading recording profile");
                    return transcodingProfileDao.loadProfileByIdSync(serverStatus.getRecordingTranscodingProfileId());
            }
            return null;
        }
    }

    public void updatePlaybackTranscodingProfile(TranscodingProfile playbackProfile) {
        new UpdateTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "playback", playbackProfile).execute();
    }

    public void updateRecordingTranscodingProfile(TranscodingProfile recordingProfile) {
        new UpdateTranscodingProfileTask(db.transcodingProfileDao(), db.serverStatusDao(), "recording", recordingProfile).execute();
    }

    protected static class UpdateTranscodingProfileTask extends AsyncTask<Void, Void, Void> {
        private String TAG = getClass().getSimpleName();
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
            Log.d(TAG, "doInBackground: profiles play: " + serverStatus.getPlaybackTranscodingProfileId() + ", rec: " + serverStatus.getRecordingTranscodingProfileId());
            switch (type) {
                case "playback":
                    Log.d(TAG, "doInBackground: playback profile id " + profile.getId());
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        Log.d(TAG, "doInBackground: inserted playback profile, id is " + id);
                        serverStatus.setPlaybackTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        Log.d(TAG, "doInBackground: updated playback profile, id is " + profile.getId());
                        serverStatus.setPlaybackTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
                case "recording":
                    Log.d(TAG, "doInBackground: recording profile id " + profile.getId());
                    if (profile.getId() == 0) {
                        int id = (int) transcodingProfileDao.insert(profile);
                        Log.d(TAG, "doInBackground: inserted recording profile, id is " + id);
                        serverStatus.setRecordingTranscodingProfileId(id);
                    } else {
                        transcodingProfileDao.update(profile);
                        Log.d(TAG, "doInBackground: updated recording profile, id is " + profile.getId());
                        serverStatus.setRecordingTranscodingProfileId(profile.getId());
                    }
                    serverStatusDao.update(serverStatus);
                    break;
            }
            return null;
        }
    }
}
