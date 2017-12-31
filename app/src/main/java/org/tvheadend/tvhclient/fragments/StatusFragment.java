package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterface;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DiscSpace;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class StatusFragment extends Fragment implements HTSListener, WakeOnLanTaskCallback {

    private Activity activity;

    // This information is always available
    @BindView(R.id.connection)
    TextView connection;
    @BindView(R.id.status)
    TextView status;
    @BindView(R.id.channels_label)
    TextView channelsLabel;
    @BindView(R.id.channels)
    TextView channels;
    @BindView(R.id.recording_label)
    TextView recordingLabel;
    @BindView(R.id.completed_recordings)
    TextView completedRec;
    @BindView(R.id.upcoming_recordings)
    TextView upcomingRec;
    @BindView(R.id.failed_recordings)
    TextView failedRec;
    @BindView(R.id.removed_recordings)
    TextView removedRec;
    @BindView(R.id.series_recordings)
    TextView seriesRec;
    @BindView(R.id.timer_recordings)
    TextView timerRec;
    @BindView(R.id.currently_recording_label)
    TextView currentlyRecLabel;
    @BindView(R.id.currently_recording)
    TextView currentlyRec;
    @BindView(R.id.discspace_label)
    TextView discspaceLabel;
    @BindView(R.id.free_discspace)
    TextView freediscspace;
    @BindView(R.id.total_discspace)
    TextView totaldiscspace;
    @BindView(R.id.server_api_version_label)
    TextView serverApiVersionLabel;
    @BindView(R.id.server_api_version)
    TextView serverApiVersion;

    private String connectionStatus = "";
    private Unbinder unbinder;
    private DatabaseHelper databaseHelper;
    private int htspVersion;
    private boolean isUnlocked;
    private MenuUtils menuUtils;
    private boolean isLoading;

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
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.status));
        }
        menuUtils = new MenuUtils(getActivity());
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        isLoading = DataStorage.getInstance().isLoading();
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            connectionStatus = bundle.getString("connection_status");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("connection_status", connectionStatus);
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        showStatus();
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
            case R.id.menu_refresh:
                menuUtils.handleMenuReconnectSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_CONNECTION_STATE_OK:
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
                        showStatus();
                    }
                });
                break;
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        isLoading = (boolean) obj;
                        showStatus();
                    }
                });
            case Constants.ACTION_DISC_SPACE:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        showStatus();
                    }
                });
                break;
        }
    }

    private void showStatus() {
        boolean show = (connectionStatus.equals(
                Constants.ACTION_CONNECTION_STATE_OK) && !isLoading);

        // The connection is ok and not loading anymore, show all data
        recordingLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        completedRec.setVisibility(show ? View.VISIBLE : View.GONE);
        upcomingRec.setVisibility(show ? View.VISIBLE : View.GONE);
        removedRec.setVisibility(show ? View.VISIBLE : View.GONE);
        failedRec.setVisibility(show ? View.VISIBLE : View.GONE);
        seriesRec.setVisibility((show && htspVersion >= 13) ? View.VISIBLE : View.GONE);
        timerRec.setVisibility((show && htspVersion >= 18 && isUnlocked) ? View.VISIBLE : View.GONE);
        channelsLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        channels.setVisibility(show ? View.VISIBLE : View.GONE);
        currentlyRecLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        currentlyRec.setVisibility(show ? View.VISIBLE : View.GONE);
        discspaceLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        freediscspace.setVisibility(show ? View.VISIBLE : View.GONE);
        totaldiscspace.setVisibility(show ? View.VISIBLE : View.GONE);
        serverApiVersionLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        serverApiVersion.setVisibility(show ? View.VISIBLE : View.GONE);

        showConnectionDetails();
        showConnectionStatus();
        showRecordingStatus();
        showDiscSpace();
        showServerStatus();

        // Show the number of available channels
        final String text = DataStorage.getInstance().getChannelsFromArray().size() + " " + getString(R.string.available);
        channels.setText(text);
    }

    /**
     * Shows the name and address of a connection, otherwise shows an
     * information that no connection is selected or available.
     */
    private void showConnectionDetails() {
        Connection connection = databaseHelper.getSelectedConnection();
        if (connection == null) {
            if (databaseHelper.getConnections().isEmpty()) {
                this.connection.setText(R.string.no_connection_available_advice);
            } else {
                this.connection.setText(R.string.no_connection_active_advice);
            }
        } else {
            String text = connection.name + " (" + connection.address + ")";
            this.connection.setText(text);
        }
    }

    /**
     * Shows the current connection status is displayed, this can be
     * authorization, timeouts or other errors.
     */
    private void showConnectionStatus() {
        // Show a textual description about the connection state
        if (isLoading) {
            status.setText(R.string.loading);
        } else {
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

        // Get the programs that are currently being recorded
        String currentRecText = "";
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

        final int seriesRecCount = DataStorage.getInstance().getSeriesRecordingsFromArray().size();
        seriesRec.setText(getResources().getQuantityString(
                R.plurals.series_recordings, seriesRecCount, seriesRecCount));

        final int timerRecCount = DataStorage.getInstance().getTimerRecordingsFromArray().size();
        timerRec.setText(getResources().getQuantityString(
                R.plurals.timer_recordings, timerRecCount, timerRecCount));
    }

    private void showServerStatus() {
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
