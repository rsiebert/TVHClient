package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConnectionRepository {

    private AppDatabase db;

    public ConnectionRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public Connection getConnectionByIdSync(int connectionId) {
        try {
            return new LoadConnectionTask(db.connectionDao(), connectionId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts a new or updates an existing connection. If the given connection
     * is marked as active, the previous active one will be set inactive
     *
     * @param connection The connection that shall be inserted or updated
     */
    public void updateConnectionSync(Connection connection) {
        if (connection.isActive()) {
            new DisableActiveConnectionTask(db.connectionDao()).execute();
        }
        new UpdateConnectionTask(db.connectionDao(), db.serverStatusDao(), connection).execute();
    }

    /**
     * Loads the currently active connections from the database
     *
     * @return The active connection or null if none exists
     */
    public Connection getActiveConnectionSync() {
        try {
            return new LoadActiveConnectionTask(db.connectionDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeConnectionSync(int id) {
        new DeleteConnectionTask(db.connectionDao(), db.serverStatusDao(), db.transcodingProfileDao(), id).execute();
    }

    public LiveData<List<Connection>> getAllConnections() {
        return db.connectionDao().loadAllConnections();
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
        private final ConnectionDao connectionDao;
        private final int id;
        private final ServerStatusDao serverStatusDao;
        private final TranscodingProfileDao profileDao;

        DeleteConnectionTask(ConnectionDao connectionDao, ServerStatusDao serverStatusDao, TranscodingProfileDao profileDao, int id) {
            this.connectionDao = connectionDao;
            this.id = id;
            this.serverStatusDao = serverStatusDao;
            this.profileDao = profileDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            connectionDao.deleteById(id);
            serverStatusDao.deleteByConnectionId(id);
            profileDao.deleteByConnectionId(id);
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
        private final ConnectionDao connectionDao;
        private final Connection connection;
        private final ServerStatusDao serverStatusDao;

        UpdateConnectionTask(ConnectionDao connectionDao, ServerStatusDao serverStatusDao, Connection connection) {
            this.connectionDao = connectionDao;
            this.connection = connection;
            this.serverStatusDao = serverStatusDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (connection.getId() > 0) {
                connectionDao.update(connection);
            } else {
                long id = connectionDao.insert(connection);
                // Create a new server status table row that is linked to the newly added connection
                ServerStatus serverStatus = new ServerStatus();
                serverStatus.setConnectionId((int) id);
                serverStatusDao.insert(serverStatus);
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
            return dao.loadConnectionByIdSync(id);
        }
    }
}
