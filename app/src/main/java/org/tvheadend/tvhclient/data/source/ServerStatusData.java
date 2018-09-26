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

import timber.log.Timber;

public class ServerStatusData extends BaseData implements DataSourceInterface<ServerStatus> {

    private final AppRoomDatabase db;

    @Inject
    public ServerStatusData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerStatus item) {
        new ItemHandlerTask(db, item, INSERT).execute();
    }

    @Override
    public void updateItem(ServerStatus item) {
        new ItemHandlerTask(db, item, UPDATE).execute();
    }

    @Override
    public void removeItem(ServerStatus item) {
        new ItemHandlerTask(db, item, DELETE).execute();
    }

    @Override
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    public LiveData<List<ServerStatus>> getLiveDataItems() {
        return null;
    }

    @Override
    public LiveData<ServerStatus> getLiveDataItemById(Object id) {
        return db.getServerStatusDao().loadServerStatusById((int) id);
    }

    @Override
    public ServerStatus getItemById(Object id) {
        try {
            return new ItemLoaderTask(db, (int) id).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @NonNull
    public List<ServerStatus> getItems() {
        return new ArrayList<>();
    }

    public LiveData<ServerStatus> getLiveDataActiveItem() {
        return db.getServerStatusDao().loadActiveServerStatus();
    }

    public ServerStatus getActiveItem() {
        Timber.d("Loading active server status");
        try {
            ServerStatus serverStatus = new ItemLoaderTask(db).execute().get();
            if (serverStatus == null) {
                Connection connection = db.getConnectionDao().loadActiveConnectionSync();
                Timber.e("Server status for active connection " + connection.getId() + " is null");
            }
            return serverStatus;
        } catch (InterruptedException e) {
            Timber.e("Failed loading active server status due to interrupt", e);
        } catch (ExecutionException e) {
            Timber.e("Failed loading active server status, execution error. Cause " + e.getCause(), e);
        }
        return null;
    }

    private static class ItemLoaderTask extends AsyncTask<Void, Void, ServerStatus> {
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
        protected ServerStatus doInBackground(Void... voids) {
            if (id < 0) {
                return db.getServerStatusDao().loadActiveServerStatusSync();
            } else {
                return db.getServerStatusDao().loadServerStatusByIdSync(id);
            }
        }
    }

    private static class ItemHandlerTask extends AsyncTask<Void, Void, Void> {
        private final AppRoomDatabase db;
        private final ServerStatus serverStatus;
        private final int type;

        ItemHandlerTask(AppRoomDatabase db, ServerStatus serverStatus, int type) {
            this.db = db;
            this.serverStatus = serverStatus;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (type) {
                case INSERT:
                    db.getServerStatusDao().insert(serverStatus);
                    break;
                case UPDATE:
                    db.getServerStatusDao().update(serverStatus);
                    break;
                case DELETE:
                    db.getServerStatusDao().delete(serverStatus);
                    break;
            }
            return null;
        }
    }
}
