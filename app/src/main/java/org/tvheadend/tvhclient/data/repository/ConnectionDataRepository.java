package org.tvheadend.tvhclient.data.repository;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.entity.Connection;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConnectionDataRepository {

    private AppDatabase db;

    public ConnectionDataRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public Connection getConnectionSync(int connectionId) {
        try {
            return new LoadConnectionTask(db.connectionDao(), connectionId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateConnectionSync(Connection connection) {
        // Make the current active connection not active
        if (connection.isActive()) {
            new DisableActiveConnectionTask(db.connectionDao()).execute();
        }
        // Insert or update the connection
        new UpdateConnectionTask(db.connectionDao(), connection).execute();
    }

    public Connection getActiveConnectionSync() {
        try {
            return new LoadActiveConnectionTask(db.connectionDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeConnectionSync(int id) {
        new DeleteConnectionTask(db.connectionDao(), id).execute();
    }

    public List<Connection> getAllConnectionsSync() {
        try {
            return new LoadAllConnectionsTask(db.connectionDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static class DeleteConnectionTask extends AsyncTask<Void, Void, Void> {
        private final ConnectionDao dao;
        private final int id;

        DeleteConnectionTask(ConnectionDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dao.deleteById(id);
            return null;
        }
    }

    protected static class LoadAllConnectionsTask extends AsyncTask<Void, Void, List<Connection>> {
        private final ConnectionDao dao;

        LoadAllConnectionsTask(ConnectionDao dao) {
            this.dao = dao;
        }

        @Override
        protected List<Connection> doInBackground(Void... voids) {
            return dao.loadAllConnectionsSync();
        }
    }

    protected static class LoadActiveConnectionTask extends AsyncTask<Void, Void, Connection> {
        private final ConnectionDao dao;

        LoadActiveConnectionTask(ConnectionDao dao) {
            this.dao = dao;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            return dao.loadActiveConnectionSync();
        }
    }

    protected static class UpdateConnectionTask extends AsyncTask<Void, Void, Void> {
        private final ConnectionDao dao;
        private final Connection connection;

        UpdateConnectionTask(ConnectionDao dao, Connection connection) {
            this.dao = dao;
            this.connection = connection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (connection.getId() > 0) {
                dao.update(connection);
            } else {
                dao.insert(connection);
            }
            return null;
        }
    }

    protected static class DisableActiveConnectionTask extends AsyncTask<Void, Void, Void> {
        private final ConnectionDao dao;

        DisableActiveConnectionTask(ConnectionDao dao) {
            this.dao = dao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dao.disableActiveConnectionSync();
            return null;
        }
    }

    protected static class LoadConnectionTask extends AsyncTask<Void, Void, Connection> {
        private final ConnectionDao dao;
        private final int id;

        LoadConnectionTask(ConnectionDao dao, int id) {
            this.dao = dao;
            this.id = id;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            return dao.loadConnectionSync(id);
        }
    }
}
