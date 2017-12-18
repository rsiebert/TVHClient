package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DiscSpace;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.tasks.WakeOnLanTaskCallback;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class StatusFragment extends Fragment implements HTSListener, WakeOnLanTaskCallback {

    private Activity activity;

    // This information is always available
    @BindView(R.id.additional_information_layout) LinearLayout additionalInformationLayout;
    @BindView(R.id.connection) TextView connection;
    @BindView(R.id.status) TextView status;
    @BindView(R.id.channel) TextView channels;
    @BindView(R.id.currently_recording) TextView currentlyRec;
    @BindView(R.id.completed_recordings) TextView completedRec;
    @BindView(R.id.upcoming_recordings) TextView upcomingRec;
    @BindView(R.id.failed_recordings) TextView failedRec;
    @BindView(R.id.removed_recordings) TextView removedRec;
    @BindView(R.id.series_recordings) TextView seriesRec;
    @BindView(R.id.timer_recordings) TextView timerRec;
    @BindView(R.id.free_discspace) TextView freediscspace;
    @BindView(R.id.total_discspace) TextView totaldiscspace;
    @BindView(R.id.server_api_version) TextView serverApiVersion;

    private String connectionStatus = "";
    private Unbinder unbinder;
    private DatabaseHelper databaseHelper;
    private int htspVersion;
    private boolean isUnlocked;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.status_fragment_layout, container, false);
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
        activity = getActivity();
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        Bundle bundle = getArguments();
        if (bundle != null) {
            connectionStatus = bundle.getString("connection_status");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);

        // Upon resume show the actual status. If stuff is loading hide certain
        // information, otherwise show the connection status and the cause of
        // possible connection problems. 
        additionalInformationLayout.setVisibility(View.GONE);
        if (DataStorage.getInstance().isLoading()) {
            onMessage(Constants.ACTION_LOADING, true);
        } else {
            onMessage(connectionStatus, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.status_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final Connection conn = databaseHelper.getSelectedConnection();
        if (isUnlocked && conn != null && conn.wol_mac_address.length() > 0) {
            menu.findItem(R.id.menu_wol).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_wol:
                final Connection connection = databaseHelper.getSelectedConnection();
                if (connection != null) {
                    WakeOnLanTask task = new WakeOnLanTask(getActivity(), this, connection);
                    task.execute();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_CONNECTION_STATE_OK:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionStatus = action;
                        showCompleteStatus();
                    }
                });
                break;
            case Constants.ACTION_CONNECTION_STATE_UNKNOWN:
            case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
            case Constants.ACTION_CONNECTION_STATE_LOST:
            case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
            case Constants.ACTION_CONNECTION_STATE_REFUSED:
            case Constants.ACTION_CONNECTION_STATE_AUTH:
            case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:
            case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionStatus = action;

                        // Hide the additional status information because the
                        // connection to the server is not OK
                        additionalInformationLayout.setVisibility(View.GONE);
                        showConnectionName();
                        showConnectionStatus();
                    }
                });
                break;
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;

                        // Show that data is being loaded from the server and hide
                        // the additional information, because this information is
                        // outdated. If the data has been loaded from the server,
                        // show the additional status layout again and display the
                        // available information.
                        if (loading) {
                            status.setText(R.string.loading);
                            additionalInformationLayout.setVisibility(View.GONE);
                            showConnectionName();

                            freediscspace.setText(R.string.loading);
                            totaldiscspace.setText(R.string.loading);
                        } else {
                            showCompleteStatus();
                        }
                    }
                });
                break;
            case Constants.ACTION_DISC_SPACE:
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded()) {
                            showDiscSpace();
                        }
                    }
                });
                break;
        }
    }

    /**
     * Displays all available status information. This is the case
     * when the loading is done and the connection is fine
     */
    private void showCompleteStatus() {
        // The connection to the server is fine again, therefore
        // show the additional information again
        additionalInformationLayout.setVisibility(View.VISIBLE);
        showConnectionName();
        showConnectionStatus();
        showRecordingStatus();
        showDiscSpace();

        // Show the number of available channels
        final String text = DataStorage.getInstance().getChannelsFromArray().size() + " " + getString(R.string.available);
        channels.setText(text);
    }

    /**
     * Shows the name and address of a connection, otherwise shows an
     * information that no connection is selected or available.
     */
    private void showConnectionName() {
        // Get the currently selected connection
        boolean noConnectionsDefined = false;
        Connection conn = null;
        if (databaseHelper != null) {
            noConnectionsDefined = databaseHelper.getConnections().isEmpty();
            conn = databaseHelper.getSelectedConnection();
        }

        // Show the details about the current connection or an information that
        // none is selected or available
        if (conn == null) {
            if (noConnectionsDefined) {
                connection.setText(R.string.no_connection_available_advice);
            } else {
                connection.setText(R.string.no_connection_active_advice);
            }
        } else {
            String text = conn.name + " (" + conn.address + ")";
            connection.setText(text);
        }
    }

    /**
     * Shows the current connection status is displayed, this can be
     * authorization, timeouts or other errors.
     */
    private void showConnectionStatus() {
        // Show a textual description about the connection state
        switch (connectionStatus) {
            case Constants.ACTION_CONNECTION_STATE_OK:
                status.setText(R.string.ready);
                break;
            case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
                status.setText(R.string.err_connect);
                break;
            case Constants.ACTION_CONNECTION_STATE_LOST:
                status.setText(R.string.err_con_lost);
                break;
            case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
                status.setText(R.string.err_con_timeout);
                break;
            case Constants.ACTION_CONNECTION_STATE_REFUSED:
            case Constants.ACTION_CONNECTION_STATE_AUTH:
                status.setText(R.string.err_auth);
                break;
            case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
                status.setText(R.string.err_no_network);
                break;
            case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:
                status.setText(R.string.no_connection_available);
                break;
            default:
                status.setText(R.string.unknown);
                break;
        }
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     */
    private void showDiscSpace() {
        DiscSpace discSpace = DataStorage.getInstance().getDiscSpace();
        if (discSpace == null) {
            freediscspace.setText(R.string.unknown);
            totaldiscspace.setText(R.string.unknown);
            return;
        }

        try {
            // Get the disc space values and convert them to megabytes
            long free = Long.valueOf(discSpace.freediskspace) / 1000000;
            long total = Long.valueOf(discSpace.totaldiskspace) / 1000000;

            String freeDiscSpace;
            String totalDiscSpace;

            // Show the free amount of disc space as GB or MB
            if (free > 1000) {
                freeDiscSpace = (free / 1000) + " GB " + getString(R.string.available);
            } else {
                freeDiscSpace = free + " MB " + getString(R.string.available);
            }
            // Show the total amount of disc space as GB or MB
            if (total > 1000) {
                totalDiscSpace = (total / 1000) + " GB " + getString(R.string.total);
            } else {
                totalDiscSpace = total + " MB " + getString(R.string.total);
            }
            freediscspace.setText(freeDiscSpace);
            totaldiscspace.setText(totalDiscSpace);
        } catch (Exception e) {
            freediscspace.setText(R.string.unknown);
            totaldiscspace.setText(R.string.unknown);
        }
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {
        String currentRecText = "";

        // Get the programs that are currently being recorded
        Map<Integer, Recording> map = DataStorage.getInstance().getRecordingsFromArray();
        for (Recording rec : map.values()) {
            if (rec.isRecording()) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                Channel channel = DataStorage.getInstance().getChannelFromArray(rec.channel);
                if (channel != null) {
                    currentRecText += " (" + getString(R.string.channel) + " " + channel.channelName + ")\n";
                }
            }
        }

        // Show which programs are being recorded
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText
                : getString(R.string.nothing));

        int completedRecCount = 0;
        int scheduledRecCount = 0;
        int failedRecCount = 0;
        int removedRecCount = 0;
        for (Recording recording : map.values()) {
            if (recording.isCompleted()) {
                completedRecCount++;
            } else if (recording.isScheduled()) {
                scheduledRecCount++;
            } else if (recording.isFailed()) {
                failedRecCount++;
            } else if (recording.isRemoved()) {
                removedRecCount++;
            }
        }

        // Show how many different recordings are available
        completedRec.setText(getResources().getQuantityString(
                R.plurals.completed_recordings, completedRecCount,
                completedRecCount));
        upcomingRec.setText(getResources().getQuantityString(
                R.plurals.upcoming_recordings, scheduledRecCount,
                scheduledRecCount));
        failedRec.setText(getResources().getQuantityString(
                R.plurals.failed_recordings, failedRecCount, failedRecCount));
        removedRec.setText(getResources().getQuantityString(
                R.plurals.removed_recordings, removedRecCount, removedRecCount));

        // Show how many series recordings are available
        if (htspVersion < Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            seriesRec.setVisibility(View.GONE);
        } else {
            final int seriesRecCount = DataStorage.getInstance().getSeriesRecordingsFromArray().size();
            seriesRec.setText(getResources().getQuantityString(
                    R.plurals.series_recordings, seriesRecCount, seriesRecCount));
        }

        // Show how many timer recordings are available if the server supports
        // it and the application is unlocked
        if (htspVersion < Constants.MIN_API_VERSION_TIMER_RECORDINGS || isUnlocked) {
            timerRec.setVisibility(View.GONE);
        } else {
            final int timerRecCount = DataStorage.getInstance().getTimerRecordingsFromArray().size();
            timerRec.setText(getResources().getQuantityString(
                    R.plurals.timer_recordings, timerRecCount, timerRecCount));
        }

        String version = String.valueOf(htspVersion)
                + "   (" + getString(R.string.server) + ": "
                + DataStorage.getInstance().getServerName() + " "
                + DataStorage.getInstance().getServerVersion() + ")";
        serverApiVersion.setText(version);
    }

    @Override
    public void notify(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
