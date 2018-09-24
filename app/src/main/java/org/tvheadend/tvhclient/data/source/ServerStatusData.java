package org.tvheadend.tvhclient.data.source;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

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
        Crashlytics.log("Trying to load active live data server status");
        return db.getServerStatusDao().loadActiveServerStatus();
    }

    public ServerStatus getActiveItem() {
        try {
            Crashlytics.log("Trying to load active server status");
            return new ItemLoaderTask(db).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
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
            Crashlytics.log("Initialized async task to load server status");
            this.db = db;
            this.id = -1;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {
            Connection connection = db.getConnectionDao().loadActiveConnectionSync();
            Crashlytics.log("Starting async task to load server status for connection " + connection.getName() + ", id " + connection.getId());
            if (id < 0) {
                Crashlytics.log("Async task done loading server status");
                ServerStatus serverStatus = db.getServerStatusDao().loadActiveServerStatusSync();
                Crashlytics.log("Server status is " + ((serverStatus == null) ? "null" : "not null"));

                if (serverStatus == null) {
                    Crashlytics.log("Server status is null, inserting new server status with connection id " + connection.getId());
                    serverStatus = new ServerStatus();
                    serverStatus.setId(connection.getId());
                    long newId = db.getServerStatusDao().insert(serverStatus);
                    Crashlytics.log("Inserted server status with new id " + newId);
                }

                return serverStatus;
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
