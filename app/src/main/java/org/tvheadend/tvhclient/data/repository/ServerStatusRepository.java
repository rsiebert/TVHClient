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
}
