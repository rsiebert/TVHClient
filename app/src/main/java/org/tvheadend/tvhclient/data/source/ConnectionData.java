package org.tvheadend.tvhclient.data.source;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import timber.log.Timber;

public class ConnectionData implements DataSourceInterface<Connection> {

    private final AppRoomDatabase db;

    @Inject
    public ConnectionData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(Connection item) {
        AsyncTask.execute(() -> {
            if (item.isActive()) {
                db.getConnectionDao().disableActiveConnection();
            }
            long newId = db.getConnectionDao().insert(item);
            // Create a new server status row in the database
            // that is linked to the newly added connection
            ServerStatus serverStatus = new ServerStatus();
            serverStatus.setConnectionId((int) newId);
            db.getServerStatusDao().insert(serverStatus);
        });
    }

    @Override
    public void updateItem(Connection item) {
        AsyncTask.execute(() -> {
            if (item.isActive()) {
                db.getConnectionDao().disableActiveConnection();
            }
            db.getConnectionDao().update(item);
        });
    }

    @Override
    public void removeItem(Connection item) {
        AsyncTask.execute(() -> {
            db.getConnectionDao().delete(item);
            db.getServerStatusDao().deleteByConnectionId(item.getId());
        });
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return db.getConnectionDao().getConnectionCount();
    }

    @Override
    public LiveData<List<Connection>> getLiveDataItems() {
        return db.getConnectionDao().loadAllConnections();
    }

    @Override
    public LiveData<Connection> getLiveDataItemById(Object id) {
        return db.getConnectionDao().loadConnectionById((int) id);
    }

    @Override
    @Nullable
    public Connection getItemById(Object id) {
        try {
            return new ConnectionByIdTask(db, (int) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading connection by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading connection by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<Connection> getItems() {
        List<Connection> connections = new ArrayList<>();
        try {
            connections.addAll(new ConnectionListTask(db).execute().get());
        } catch (InterruptedException e) {
            Timber.d("Loading all connections task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading all connections task aborted", e);
        }
        return connections;
    }

    @Nullable
    public Connection getActiveItem() {
        try {
            return new ConnectionByIdTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading active connection task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading active connection task aborted", e);
        }
        return null;
    }

    static class ConnectionByIdTask extends AsyncTask<Void, Void, Connection> {
        private final AppRoomDatabase db;
        private final int id;

        ConnectionByIdTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        ConnectionByIdTask(AppRoomDatabase db) {
            this.db = db;
            this.id = -1;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            if (id < 0) {
                return db.getConnectionDao().loadActiveConnectionSync();
            } else {
                return db.getConnectionDao().loadConnectionByIdSync(id);
            }
        }
    }

    private static class ConnectionListTask extends AsyncTask<Void, Void, List<Connection>> {
        private final AppRoomDatabase db;

        ConnectionListTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Connection> doInBackground(Void... voids) {
            return db.getConnectionDao().loadAllConnectionsSync();
        }
    }
}
