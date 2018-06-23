package org.tvheadend.tvhclient.features.shared;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailableInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class BaseFragment extends Fragment implements NetworkAvailableInterface {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected boolean isDualPane;
    protected MenuUtils menuUtils;
    protected boolean isUnlocked;
    protected int htspVersion;
    protected boolean isNetworkAvailable;

    protected Connection connection;
    protected ServerStatus serverStatus;

    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        MainApplication.getComponent().inject(this);

        connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());

        htspVersion = serverStatus.getHtspVersion();
        isUnlocked = MainApplication.getInstance().isUnlocked();
        menuUtils = new MenuUtils(activity);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = activity.findViewById(R.id.right_fragment);
        isDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                menuUtils.handleMenuReconnectSelection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNetworkIsAvailable(boolean networkIsAvailable) {
        Timber.d("Network is available " + networkIsAvailable + ", invalidating menu");
        isNetworkAvailable = networkIsAvailable;
        activity.invalidateOptionsMenu();
    }
}
