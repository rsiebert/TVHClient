package org.tvheadend.tvhclient.ui.settings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;

public class ConnectionViewModel extends AndroidViewModel {
    private String TAG = getClass().getSimpleName();

    private final ConnectionDataRepository repository;
    private Connection connection;

    public ConnectionViewModel(Application application) {
        super(application);
        repository = new ConnectionDataRepository(application);
    }

    Connection getConnectionByIdSync(int connectionId) {
        if (connection == null) {
            if (connectionId > 0) {
                connection = repository.getConnectionByIdSync(connectionId);
            } else {
                connection = new Connection();
            }
        }
        return connection;
    }
}
