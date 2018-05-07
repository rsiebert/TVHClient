package org.tvheadend.tvhclient.features.settings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ConnectionViewModel extends AndroidViewModel {
    private final LiveData<List<Connection>> connections;
    @Inject
    protected AppRepository appRepository;
    private Connection connection;

    public ConnectionViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
        connections = appRepository.getConnectionData().getLiveDataItems();
    }

    Connection getConnectionByIdSync(int connectionId) {
        if (connection == null) {
            if (connectionId >= 0) {
                connection = appRepository.getConnectionData().getItemById(connectionId);
            } else {
                connection = new Connection();
            }
        }
        return connection;
    }

    LiveData<List<Connection>> getAllConnections() {
        return connections;
    }
}
