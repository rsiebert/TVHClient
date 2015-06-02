package org.tvheadend.tvhclient.fragments;

import java.util.Map;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatusFragment extends Fragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = StatusFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;

    private LinearLayout additionalInformationLayout;

	// This information is always available
    private TextView connection;
    private TextView status;
    private TextView channels;
	private TextView currentlyRec;
	private TextView completedRec;
	private TextView upcomingRec;
	private TextView failedRec;
    private TextView freediscspace;
    private TextView totaldiscspace;
    private TextView serverApiVersion;

    // This information depends on the server capabilities
	private TextView seriesRecLabel;
	private TextView seriesRec;
	private TextView timerRecLabel;
    private TextView timerRec;

    private String connectionStatus = "";
    private String freeDiscSpace = "";
    private String totalDiscSpace = "";

    private TVHClientApplication app;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            connectionStatus = bundle.getString(Constants.BUNDLE_CONNECTION_STATUS);
        }

        View v = inflater.inflate(R.layout.status_fragment_layout, container, false);
        connection = (TextView) v.findViewById(R.id.connection);
        status = (TextView) v.findViewById(R.id.status);
        additionalInformationLayout = (LinearLayout) v.findViewById(R.id.additional_information_layout);
        channels = (TextView) v.findViewById(R.id.channels);
        currentlyRec = (TextView) v.findViewById(R.id.currently_recording);
        completedRec = (TextView) v.findViewById(R.id.completed_recordings);
        upcomingRec = (TextView) v.findViewById(R.id.upcoming_recordings);
        failedRec = (TextView) v.findViewById(R.id.failed_recordings);
        freediscspace = (TextView) v.findViewById(R.id.free_discspace);
        totaldiscspace = (TextView) v.findViewById(R.id.total_discspace);
        seriesRecLabel = (TextView) v.findViewById(R.id.series_recording_label);
        seriesRec = (TextView) v.findViewById(R.id.series_recordings);
        timerRecLabel = (TextView) v.findViewById(R.id.timer_recording_label);
        timerRec = (TextView) v.findViewById(R.id.timer_recordings);
        serverApiVersion = (TextView) v.findViewById(R.id.server_api_version);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.status));
            actionBarInterface.setActionBarSubtitle("");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addListener(this);

        // Upon resume show the actual status. If stuff is loading hide certain
        // information, otherwise show the connection status and the cause of
        // possible connection problems. 
        additionalInformationLayout.setVisibility(View.GONE);
        if (app.isLoading()) {
            onMessage(Constants.ACTION_LOADING, true);
        } else {
            onMessage(connectionStatus, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onDetach() {
        actionBarInterface = null;
        super.onDetach();
    }

	@Override
	public void onMessage(final String action, final Object obj) {
	    if (action.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
	        activity.runOnUiThread(new Runnable() {
                public void run() {
                    connectionStatus = action;

                    // The connection to the server is fine again, therefore
                    // show the additional information again
                    additionalInformationLayout.setVisibility(View.VISIBLE);
                    showConnectionStatus();
                    channels.setText(app.getChannels().size() + " " + getString(R.string.available));
                    showRecordingStatus();

                    // Also get the disc space in case it was not yet retrieved
                    getDiscSpace();
                }
            });
	    } else if (action.equals(Constants.ACTION_CONNECTION_STATE_UNKNOWN)
	            || action.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)
	            || action.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || action.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)
                || action.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)
                || action.equals(Constants.ACTION_CONNECTION_STATE_AUTH)
                || action.equals(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION)
                || action.equals(Constants.ACTION_CONNECTION_STATE_NO_NETWORK)) {
	        activity.runOnUiThread(new Runnable() {
                public void run() {
                    connectionStatus = action;

                    // Hide the additional status information because the
                    // connection to the server is not OK
                    additionalInformationLayout.setVisibility(View.GONE);
                    showConnectionStatus();
                }
            });
        } else if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (actionBarInterface != null) {
                        final String updating = (loading ? getString(R.string.updating) : "");
                        actionBarInterface.setActionBarSubtitle(updating);
                    }

                    // Show that data is being loaded from the server and hide
                    // the additional information, because this information is
                    // outdated. If the data has been loaded from the server,
                    // show the additional status layout again and display the
                    // available information. 
                    if (loading) {
                        status.setText(getString(R.string.loading));
                        additionalInformationLayout.setVisibility(View.GONE);
                    } else {
                        additionalInformationLayout.setVisibility(View.VISIBLE);
                        showConnectionStatus();
                        channels.setText(app.getChannels().size() + " " + getString(R.string.available));
                        showRecordingStatus();

                        // After the data has been loaded, the server accepts
                        // new service calls, get the disc space information 
                        // from the server 
                        getDiscSpace();
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_DISC_SPACE)) {
            activity.runOnUiThread(new Runnable() {
                @SuppressWarnings("unchecked")
                public void run() {
                	showDiscSpace((Map<String, String>) obj);
                }
            });
        }
	}

    /**
     * Shows the name and address of a connection, otherwise shows an
     * information that no connection is selected or available. Additionally the
     * current connection status is displayed, this can be authorization,
     * timeouts or other errors.
     */
    protected void showConnectionStatus() {

        // Get the currently selected connection
        boolean noConnectionsDefined = false;
        Connection conn = null;
        if (DatabaseHelper.getInstance() != null) {
            noConnectionsDefined = DatabaseHelper.getInstance().getConnections().isEmpty();
            conn = DatabaseHelper.getInstance().getSelectedConnection();
        }

        // Show the details about the current connection or an information that
        // none is selected or available
        if (conn == null) {
            if (noConnectionsDefined) {
                connection.setText(getString(R.string.no_connection_available_advice));
            } else {
                connection.setText(getString(R.string.no_connection_active_advice));
            }
        } else {
            connection.setText(conn.name + " (" + conn.address + ")");
        }

        // Show a textual description about the connection state
        if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            status.setText(getString(R.string.ready));
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)) {
            status.setText(R.string.err_connect);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_LOST)) {
            status.setText(R.string.err_con_lost);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)) {
            status.setText(R.string.err_con_timeout);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_REFUSED) 
                || connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
            status.setText(R.string.err_auth);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_NO_NETWORK)) {
            status.setText(getString(R.string.err_no_network));
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION)) {
            status.setText(getString(R.string.no_connection_available));
        } else {
            status.setText(getString(R.string.unknown));
        }
    }

    /**
     * Calls the service to get the available disc space information from the
     * server. Additionally sets the text views to loading until the data has
     * been received.
     */
    private void getDiscSpace() {
        freediscspace.setText(getString(R.string.loading));
        totaldiscspace.setText(getString(R.string.loading));
        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_GET_DISC_SPACE);
        activity.startService(intent);
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     * 
     * @param obj
     */
    private void showDiscSpace(final Map<String, String> list) {
        try {
            // Get the disc space values and convert them to megabytes
            long free = (Long.parseLong(list.get("freediskspace")) / 1000000);
            long total = (Long.parseLong(list.get("totaldiskspace")) / 1000000);

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
            freediscspace.setText(getString(R.string.unknown));
            totaldiscspace.setText(getString(R.string.unknown));
        }
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {
        String currentRecText = "";

        // Get the programs that are currently being recorded
        for (Recording rec : app.getRecordings()) {
            if (rec.isRecording() == true) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                if (rec.channel != null) {
                    currentRecText += " (" + getString(R.string.channel) + " " + rec.channel.name + ")\n";
                }
            }
        }

        // Show which programs are being recorded
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText
                : getString(R.string.nothing));

        final int completedRecCount = app.getRecordingsByType(
                Constants.RECORDING_TYPE_COMPLETED).size();
        final int scheduledRecCount = app.getRecordingsByType(
                Constants.RECORDING_TYPE_SCHEDULED).size();
        final int failedRecCount = app.getRecordingsByType(
                Constants.RECORDING_TYPE_FAILED).size();

        // Show how many different recordings are available
        completedRec.setText(getResources().getQuantityString(
                R.plurals.completed_recordings, completedRecCount,
                completedRecCount));
        upcomingRec.setText(getResources().getQuantityString(
                R.plurals.upcoming_recordings, scheduledRecCount,
                scheduledRecCount));
        failedRec.setText(getResources().getQuantityString(
                R.plurals.failed_recordings, failedRecCount, failedRecCount));

        // Show how many series recordings are available
        if (app.getProtocolVersion() < Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            seriesRecLabel.setVisibility(View.GONE);
            seriesRec.setVisibility(View.GONE);
        } else {
            seriesRec.setText(app.getSeriesRecordings().size() + " " + getString(R.string.available));
        }

        // Show how many timer recordings are available if the server supports
        // it and the application is unlocked
        if (app.getProtocolVersion() < Constants.MIN_API_VERSION_SERIES_RECORDINGS || !app.isUnlocked()) {
            timerRecLabel.setVisibility(View.GONE);
            timerRec.setVisibility(View.GONE);
        } else {
            timerRec.setText(app.getTimerRecordings().size() + " " + getString(R.string.available));
        }

        serverApiVersion.setText(String.valueOf(app.getProtocolVersion())
                + "   (" + getString(R.string.server) + ": "
                + app.getServerName() + " " + app.getServerVersion() + ")");
    }
}
