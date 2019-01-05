package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

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
    public Connection getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<Connection> getItems() {
        List<Connection> connections = new ArrayList<>();
        try {
            connections.addAll(new ItemsLoaderTask(db).execute().get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return connections;
    }

    public Connection getActiveItem() {
        try {
            return new ItemLoaderTask(db).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, Connection> {
        private final AppRoomDatabase db;
        private final int id;

        ItemLoaderTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        ItemLoaderTask(AppRoomDatabase db) {
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

    private static class ItemsLoaderTask extends AsyncTask<Void, Void, List<Connection>> {
        private final AppRoomDatabase db;

        ItemsLoaderTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected List<Connection> doInBackground(Void... voids) {
            return db.getConnectionDao().loadAllConnectionsSync();
        }
    }
}
