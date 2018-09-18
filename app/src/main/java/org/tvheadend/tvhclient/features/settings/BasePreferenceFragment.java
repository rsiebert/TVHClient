package org.tvheadend.tvhclient.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

import javax.inject.Inject;

public class BasePreferenceFragment extends PreferenceFragment {

    AppCompatActivity activity;
    ToolbarInterface toolbarInterface;
    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;
    boolean isUnlocked;
    int htspVersion;
    ServerStatus serverStatus;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        MainApplication.getComponent().inject(this);

        serverStatus = appRepository.getServerStatusData().getActiveItem();
        htspVersion = serverStatus.getHtspVersion();
        isUnlocked = MainApplication.getInstance().isUnlocked();
    }
}
