package org.tvheadend.tvhclient.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

import javax.inject.Inject;

public class BasePreferenceFragment extends PreferenceFragment {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;
    protected ConfigRepository configRepository;
    protected ConnectionRepository connectionRepository;
    protected boolean isUnlocked;
    protected int htspVersion;
    protected ServerStatus serverStatus;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        MainApplication.getComponent().inject(this);

        configRepository = new ConfigRepository(activity);
        connectionRepository = new ConnectionRepository(activity);

        Connection connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());
        isUnlocked = MainApplication.getInstance().isUnlocked();
        htspVersion = serverStatus.getHtspVersion();
    }
}
