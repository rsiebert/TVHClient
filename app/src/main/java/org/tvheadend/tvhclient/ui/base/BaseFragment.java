package org.tvheadend.tvhclient.ui.base;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;
import org.tvheadend.tvhclient.utils.MenuUtils;

public class BaseFragment extends Fragment {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected boolean isDualPane;
    protected RecordingRepository repository;
    protected SharedPreferences sharedPreferences;
    protected MenuUtils menuUtils;
    protected boolean isUnlocked;
    protected ServerStatus serverStatus;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        repository = new RecordingRepository(activity);
        serverStatus = new ConfigRepository(activity).getServerStatus();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        menuUtils = new MenuUtils(activity);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = activity.findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        setHasOptionsMenu(true);
    }
}
