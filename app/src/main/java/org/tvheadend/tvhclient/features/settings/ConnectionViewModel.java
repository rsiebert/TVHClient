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

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    public ConnectionViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
    }

    public LiveData<List<Connection>> getAllConnections() {
        return appRepository.getConnectionData().getLiveDataItems();
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
