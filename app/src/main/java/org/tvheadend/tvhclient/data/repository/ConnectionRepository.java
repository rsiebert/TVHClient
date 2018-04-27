package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.local.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.local.dao.ServerStatusDao;
import org.tvheadend.tvhclient.data.local.dao.TranscodingProfileDao;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class ConnectionRepository {

    private AppRoomDatabase db;

    public ConnectionRepository(Context context) {
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
    }

    public Connection getConnectionByIdSync(int connectionId) {
        try {
            return new LoadConnectionTask(db.getConnectionDao(), connectionId).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing connection. If the given connection
     * is marked as active, the previous active one will be set inactive
     *
     * @param connection The connection that shall be inserted or updated
     */
    public void updateConnectionSync(Connection connection) {
        if (connection.isActive()) {
            new DisableActiveConnectionTask(db.getConnectionDao()).execute();
        }
        new UpdateConnectionTask(db.getConnectionDao(), connection).execute();
    }

    public void insertConnectionSync(Connection connection) {
        if (connection.isActive()) {
            new DisableActiveConnectionTask(db.getConnectionDao()).execute();
        }
        new InsertConnectionTask(db.getConnectionDao(), db.getServerStatusDao(), connection).execute();
    }

    /**
     * Loads the currently active connections from the database
     *
     * @return The active connection or null if none exists
     */
    public Connection getActiveConnectionSync() {
        try {
            return new LoadActiveConnectionTask(db.getConnectionDao()).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeConnectionSync(int id) {
        new DeleteConnectionTask(db.getConnectionDao(), db.getServerStatusDao(), db.getTranscodingProfileDao(), id).execute();
    }

    public LiveData<List<Connection>> getAllConnections() {
        return db.getConnectionDao().loadAllConnections();
    }

    public List<Connection> getAllConnectionsSync() {
        try {
            return new LoadAllConnectionsTask(db.getConnectionDao()).execute().get();
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

        UpdateConnectionTask(ConnectionDao connectionDao, Connection connection) {
            this.connectionDao = connectionDao;
            this.connection = connection;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            connectionDao.update(connection);
            Timber.d("Updated connection with id " + connection.getId());
            return null;
        }
    }

    protected static class InsertConnectionTask extends AsyncTask<Void, Void, Void> {
        private final ConnectionDao connectionDao;
        private final Connection connection;
        private final ServerStatusDao serverStatusDao;

        InsertConnectionTask(ConnectionDao connectionDao, ServerStatusDao serverStatusDao, Connection connection) {
            this.connectionDao = connectionDao;
            this.connection = connection;
            this.serverStatusDao = serverStatusDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            long id = connectionDao.insert(connection);
            Timber.d("Inserted connection with new id " + id);
            // Create a new server status table row that is linked to the newly added connection
            ServerStatus serverStatus = new ServerStatus();
            serverStatus.setConnectionId((int) id);
            serverStatusDao.insert(serverStatus);
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
