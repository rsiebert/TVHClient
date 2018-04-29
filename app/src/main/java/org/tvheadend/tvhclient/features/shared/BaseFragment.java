package org.tvheadend.tvhclient.features.shared;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

public class BaseFragment extends Fragment {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected boolean isDualPane;
    protected SharedPreferences sharedPreferences;
    protected MenuUtils menuUtils;
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
        isUnlocked = MainApplication.getInstance().isUnlocked();
        htspVersion = new ConfigRepository(activity).getServerStatus().getHtspVersion();
        menuUtils = new MenuUtils(activity);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = activity.findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        setHasOptionsMenu(true);
    }
}
