package org.tvheadend.tvhclient.ui.misc;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.DiscSpace;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO remove subtitle

public class StatusFragment extends Fragment implements WakeOnLanTaskCallback {

    private Activity activity;

    // This information is always available
    @BindView(R.id.connection)
    TextView connection;
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

    private Unbinder unbinder;
    private DatabaseHelper databaseHelper;
    private int htspVersion;
    private boolean isUnlocked;
    private MenuUtils menuUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.status_fragment, container, false);
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
            toolbarInterface.setSubtitle(null);
        }
        menuUtils = new MenuUtils(getActivity());
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.status_options_menu, menu);
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
    public void onResume() {
        super.onResume();

        // The connection is ok and not loading anymore, show all data
        seriesRec.setVisibility((htspVersion >= 13) ? View.VISIBLE : View.GONE);
        timerRec.setVisibility((htspVersion >= 18 && isUnlocked) ? View.VISIBLE : View.GONE);

        showConnectionDetails();
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
        StringBuilder currentRecText = new StringBuilder();
        Map<Integer, Recording> map = DataStorage.getInstance().getRecordingsFromArray();
        for (Recording rec : map.values()) {
            if (rec.isRecording()) {
                currentRecText.append(getString(R.string.currently_recording)).append(": ").append(rec.title);
                Channel channel = DataStorage.getInstance().getChannelFromArray(rec.channel);
                if (channel != null) {
                    currentRecText.append(" (").append(getString(R.string.channel)).append(" ").append(channel.channelName).append(")\n");
                }
            }
        }

        // Show which programs are being recorded
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText.toString()
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
