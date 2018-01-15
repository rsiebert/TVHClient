package org.tvheadend.tvhclient.ui.startup;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.service.HTSConnection;
import org.tvheadend.tvhclient.service.HTSService;
import org.tvheadend.tvhclient.ui.NavigationActivity;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.settings.SettingsActivity;
import org.tvheadend.tvhclient.utils.MenuUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO loading initial data is shown before auth message

public class SyncStatusFragment extends Fragment {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status_text)
    TextView statusTextView;
    @BindView(R.id.status_background)
    ImageView statusImageView;

    private String status;
    private Unbinder unbinder;
    private SharedPreferences sharedPreferences;
    private MenuUtils menuUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sync_status_fragment, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) getActivity();
            toolbarInterface.setTitle("Sync Status");
        }
        // Allow showing the toolbar menu with the settings menu
        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuUtils = new MenuUtils(getActivity());

        // Restore the last shown state when an orientation change happened.
        // Otherwise start the service when the fragment was created first
        if (savedInstanceState != null) {
            status = savedInstanceState.getString("status");
        } else {
            // TODO
            //Intent intent = new Intent(getActivity(), EpgSyncService.class);
            //getActivity().stopService(intent);
            //getActivity().startService(intent);

            //boolean initialSyncDone = sharedPreferences.getBoolean("initial_sync_done", false);
            //if (!initialSyncDone) {
                Intent intent = new Intent(getActivity(), HTSService.class);
                intent.setAction("connect");
                getActivity().startService(intent);
            //} else {
            //    showContentScreen();
            //}
        }
        statusTextView.setText(status);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.startup_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showSettingsActivity();
                return true;
            case R.id.menu_refresh:
                menuUtils.handleMenuReconnectSelection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("status", status);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("service_status");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the connection status from the local broadcast

            if (intent.hasExtra("connection_status")) {
                progressBar.setVisibility(View.GONE);
                HTSConnection.State state = (HTSConnection.State) intent.getSerializableExtra("connection_status");
                if (state == HTSConnection.State.CLOSED) {
                    status = "Connection closed";
                } else if (state == HTSConnection.State.CLOSING) {
                    status = "Closing connection...";
                } else if (state == HTSConnection.State.CONNECTED) {
                    status = "Connected";
                } else if (state == HTSConnection.State.CONNECTING) {
                    status = "Connecting...";
                } else if (state == HTSConnection.State.FAILED) {
                    status = "Connection failed";
                } else if (state == HTSConnection.State.FAILED_UNRESOLVED_ADDRESS) {
                    status = "Could not resolve address";
                } else if (state == HTSConnection.State.FAILED_INTERRUPTED) {
                    status = "Connection attempt was interrupted";
                } else if (state == HTSConnection.State.FAILED_EXCEPTION_OPENING_SOCKET) {
                    status = "Connection failed, could not open socket";
                } else if (state == HTSConnection.State.FAILED_CONNECTING_TO_SERVER) {
                    status = "Could not connect to server";
                } else {
                    status = "Unknown connection state";
                    stopService();
                }
            }
            // Get the current authentication status from the local broadcast
            if (intent.hasExtra("authentication_status")) {
                progressBar.setVisibility(View.GONE);
                HTSConnection.State state = (HTSConnection.State) intent.getSerializableExtra("authentication_status");
                if (state == HTSConnection.State.AUTHENTICATING) {
                    status = "Authenticating...";
                } else if (state == HTSConnection.State.AUTHENTICATED) {
                    status = "Authenticated";
                } else if (state == HTSConnection.State.FAILED) {
                    status = "Authentication failed";
                    stopService();
                } else {
                    status = "Unknown authentication state";
                    stopService();
                }
            }

            // Inform the fragment about the current sync status
            if (intent.hasExtra("sync_status")) {
                progressBar.setVisibility(View.VISIBLE);
                // TODO get an enum
                status = intent.getStringExtra("sync_status");
                // When this is received the connection is successfully established with
                // the server and any possible sync has been finished
                if (intent.getStringExtra("sync_status").equals("done")) {
                    showContentScreen();
                }
            }

            statusTextView.setText(status);
        }
    };

    private void stopService() {
        // Unregister from the broadcast manager to avoid getting any connection
        // state changes when the service is stopped and the connection gets closed.
        // The user needs to go to the settings and fix the login credentials.
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
        // TODO getActivity().stopService(new Intent(getActivity(), EpgSyncService.class));

        Intent intent = new Intent(getActivity(), HTSService.class);
        intent.setAction("disconnect");
        getActivity().startService(intent);
    }

    private void showContentScreen() {
        // Get the initial screen from the user preference.
        // This determines which screen shall be shown first
        int startScreen = Integer.parseInt(sharedPreferences.getString("defaultMenuPositionPref", "0"));
        Intent intent = new Intent(getActivity(), NavigationActivity.class);
        intent.putExtra("navigation_menu_position", startScreen);
        getActivity().finish();
        getActivity().startActivity(intent);
    }

    private void showSettingsActivity() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        intent.putExtra("initial_setup", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        getActivity().finish();
    }
}
