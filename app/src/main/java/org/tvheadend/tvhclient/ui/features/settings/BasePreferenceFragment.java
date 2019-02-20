package org.tvheadend.tvhclient.ui.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;

public class BasePreferenceFragment extends PreferenceFragment {

    AppCompatActivity activity;
    ToolbarInterface toolbarInterface;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    AppRepository appRepository;
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
