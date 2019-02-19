package org.tvheadend.tvhclient.ui.features.settings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ConnectionViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    private final LiveData<List<Connection>> connections;

    public ConnectionViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        connections = appRepository.getConnectionData().getLiveDataItems();
    }

    public LiveData<List<Connection>> getAllConnections() {
        return connections;
    }

    void setConnectionHasChanged(boolean change) {
        sharedPreferences.edit().putBoolean("connection_value_changed", change).apply();
    }

    int getActiveConnectionId() {
        Connection connection = appRepository.getConnectionData().getActiveItem();
        return (connection != null ? connection.getId() : -1);
    }

    boolean getConnectionHasChanged() {
        return sharedPreferences.getBoolean("connection_value_changed", false);
    }
}
