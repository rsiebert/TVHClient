package org.tvheadend.tvhclient.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

public class BasePreferenceFragment extends PreferenceFragment {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected SharedPreferences sharedPreferences;
    protected ConfigRepository configRepository;
    protected ConnectionRepository connectionRepository;
    protected boolean isUnlocked;
    protected int htspVersion;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        configRepository = new ConfigRepository(activity);
        connectionRepository = new ConnectionRepository(activity);
        isUnlocked = MainApplication.getInstance().isUnlocked();
        htspVersion = configRepository.getServerStatus().getHtspVersion();
    }
}
