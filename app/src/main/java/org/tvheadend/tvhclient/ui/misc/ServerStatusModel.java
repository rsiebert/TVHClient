package org.tvheadend.tvhclient.ui.misc;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ServerStatusRepository;

public class ServerStatusModel extends AndroidViewModel {

    private final ServerStatusRepository serverRepository;
    private LiveData<ServerStatus> serverStatus;

    public ServerStatusModel(@NonNull Application application) {
        super(application);
        serverRepository = new ServerStatusRepository(application);
        serverStatus = serverRepository.loadServerStatus();
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverStatus;
    }
}
