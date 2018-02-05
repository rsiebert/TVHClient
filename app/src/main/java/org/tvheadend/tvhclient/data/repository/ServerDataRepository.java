package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.concurrent.ExecutionException;

public class ServerDataRepository {
    private final AppDatabase db;

    public ServerDataRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public ServerStatus loadServerStatus() {
        try {
            return new LoadServerStatusTask(db.serverStatusDao()).execute().get();
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

    public void updatePlaybackServerProfile(int id) {
        new UpdateProfileTask(db.serverStatusDao(), "playback_server_profile", id).execute();
    }

    public void updateRecordingServerProfile(int id) {
        new UpdateProfileTask(db.serverStatusDao(), "recording_server_profile", id).execute();
    }

    public void updateCastingServerProfile(int id) {
        new UpdateProfileTask(db.serverStatusDao(), "casting_server_profile", id).execute();
    }

    public void updatePlaybackTranscodingProfile(int id) {
        new UpdateProfileTask(db.serverStatusDao(), "playback_transcoding_profile", id).execute();
    }

    public void updateRecordingTranscodingProfile(int id) {
        new UpdateProfileTask(db.serverStatusDao(), "recording_transcoding_profile", id).execute();
    }

    private static class UpdateProfileTask extends AsyncTask<Void, Void, Void> {
        private final ServerStatusDao dao;
        private final int id;
        private final String type;

        UpdateProfileTask(ServerStatusDao dao, String type, int id) {
            this.dao = dao;
            this.id = id;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case "playback_server_profile":
                    dao.updatePlaybackServerProfile(id);
                case "recording_server_profile":
                    dao.updateRecordingServerProfile(id);
                case "casting_server_profile":
                    dao.updateCastingServerProfile(id);
                case "playback_transcoding_profile":
                    dao.updatePlaybackTranscodingProfile(id);
                case "recording_transcoding_profile":
                    dao.updateRecordingTranscodingProfile(id);
            }
            return null;
        }
    }
}
