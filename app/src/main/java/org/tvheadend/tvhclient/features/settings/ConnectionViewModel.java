package org.tvheadend.tvhclient.features.settings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ConnectionViewModel extends AndroidViewModel {

    private final LiveData<List<Connection>> connections;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    public ConnectionViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
        connections = appRepository.getConnectionData().getLiveDataItems();
    }

    LiveData<List<Connection>> getAllConnections() {
        return connections;
    }

    public void setConnectionHasChanged(boolean change) {
        sharedPreferences.edit().putBoolean("connection_value_changed", change).apply();
    }

    public int getActiveConnectionId() {
        Connection connection = appRepository.getConnectionData().getActiveItem();
        return (connection != null ? connection.getId() : -1);
    }

    public boolean getConnectionHasChanged() {
        return sharedPreferences.getBoolean("connection_value_changed", false);
    }
}
