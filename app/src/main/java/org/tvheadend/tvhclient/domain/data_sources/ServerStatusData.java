package org.tvheadend.tvhclient.domain.data_sources;

import androidx.lifecycle.LiveData;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class ServerStatusData implements DataSourceInterface<ServerStatus> {

    private final AppRoomDatabase db;

    public ServerStatusData(AppRoomDatabase database) {
        this.db = database;
    }

    @Override
    public void addItem(ServerStatus item) {
        AsyncTask.execute(() -> db.getServerStatusDao().insert(item));
    }

    @Override
    public void updateItem(ServerStatus item) {
        AsyncTask.execute(() -> db.getServerStatusDao().update(item));
    }

    @Override
    public void removeItem(ServerStatus item) {
        AsyncTask.execute(() -> db.getServerStatusDao().delete(item));
    }

    @Override
    @Nullable
    public LiveData<Integer> getLiveDataItemCount() {
        return null;
    }

    @Override
    @Nullable
    public LiveData<List<ServerStatus>> getLiveDataItems() {
        return null;
    }

    @Override
    @Nullable
    public LiveData<ServerStatus> getLiveDataItemById(@NonNull Object id) {
        return db.getServerStatusDao().loadServerStatusById((int) id);
    }

    @Override
    public ServerStatus getItemById(@NonNull Object id) {
        try {
            return new ServerStatusByIdTask(db, (int) id).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading server status by id task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading server status by id task aborted", e);
        }
        return null;
    }

    @Override
    @NonNull
    public List<ServerStatus> getItems() {
        return new ArrayList<>();
    }

    @Nullable
    public LiveData<ServerStatus> getLiveDataActiveItem() {
        return db.getServerStatusDao().loadActiveServerStatus();
    }

    @NonNull
    public ServerStatus getActiveItem() {
        Timber.d("Loading active server status");
        ServerStatus serverStatus = new ServerStatus();
        try {
            return new ActiveServerStatusTask(db).execute().get();
        } catch (InterruptedException e) {
            Timber.d("Loading active server status task got interrupted", e);
        } catch (ExecutionException e) {
            Timber.d("Loading active server status task aborted", e);
        }
        return serverStatus;
    }

    private static class ServerStatusByIdTask extends AsyncTask<Void, Void, ServerStatus> {
        private final AppRoomDatabase db;
        private final int id;

        ServerStatusByIdTask(AppRoomDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {
            return db.getServerStatusDao().loadServerStatusByIdSync(id);
        }
    }

    private static class ActiveServerStatusTask extends AsyncTask<Void, Void, ServerStatus> {
        private final AppRoomDatabase db;

        ActiveServerStatusTask(AppRoomDatabase db) {
            this.db = db;
        }

        @Override
        protected ServerStatus doInBackground(Void... voids) {

            ServerStatus serverStatus = db.getServerStatusDao().loadActiveServerStatusSync();
            if (serverStatus == null) {
                String msg = "Trying to get active server status from database returned no entry.";
                Timber.e(msg);
                if (Fabric.isInitialized()) {
                    Crashlytics.logException(new Exception(msg));
                }

                Connection connection = db.getConnectionDao().loadActiveConnectionSync();
                if (connection != null) {
                    serverStatus = new ServerStatus();
                    serverStatus.setConnectionId(connection.getId());
                    db.getServerStatusDao().insert(serverStatus);

                    msg = "Trying to get active server status from database returned no entry.\n" +
                            "Inserted new server status for active connection " + connection.getId();
                    Timber.e(msg);
                    if (Fabric.isInitialized()) {
                        Crashlytics.logException(new Exception(msg));
                    }
                } else {
                    msg = "Trying to get active server status from database returned no entry.\n" +
                            "loading active connection to add a new server status also returned no entry";
                    Timber.e(msg);
                    if (Fabric.isInitialized()) {
                        Crashlytics.logException(new Exception(msg));
                    }
                }
            }
            return serverStatus;
        }
    }
}
