package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.concurrent.ExecutionException;

public class ServerStatusRepository {
    private final AppDatabase db;

    public ServerStatusRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public ServerStatus loadServerStatusSync() {
        try {
            return new LoadServerStatusTask(db.serverStatusDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateSelectedChannelTag(int id) {
        ServerStatus serverStatus = loadServerStatusSync();
        serverStatus.setChannelTagId(id);
        new UpdateProfileTask(db.serverStatusDao(), serverStatus).execute();
    }

    public LiveData<ServerStatus> loadServerStatus() {
        return db.serverStatusDao().loadServerStatus();
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

    private static class UpdateProfileTask extends AsyncTask<Void, Void, Void> {
        private final ServerStatusDao dao;
        private final ServerStatus serverStatus;

        UpdateProfileTask(ServerStatusDao dao, ServerStatus serverStatus) {
            this.dao = dao;
            this.serverStatus = serverStatus;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dao.update(serverStatus);
            return null;
        }
    }
}
