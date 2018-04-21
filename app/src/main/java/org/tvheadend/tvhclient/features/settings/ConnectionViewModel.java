package org.tvheadend.tvhclient.features.settings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;

import java.util.List;

public class ConnectionViewModel extends AndroidViewModel {
    private final LiveData<List<Connection>> connections;
    private final ConnectionRepository repository;
    private Connection connection;

    public ConnectionViewModel(Application application) {
        super(application);
        repository = new ConnectionRepository(application);
        connections = repository.getAllConnections();
    }

    Connection getConnectionByIdSync(int connectionId) {
        if (connection == null) {
            if (connectionId >= 0) {
                connection = repository.getConnectionByIdSync(connectionId);
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
