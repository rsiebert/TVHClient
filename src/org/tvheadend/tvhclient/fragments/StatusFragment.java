/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import android.widget.TextView;

public class StatusFragment extends Fragment implements HTSListener {

    private final static String TAG = StatusFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;

    private TextView connection;
    private TextView status;
    private TextView discspaceLabel;
	private TextView freediscspace;
	private TextView totaldiscspace;
	private TextView channelLabel;
	private TextView channels;
	private TextView currentlyRecLabel;
	private TextView currentlyRec;
	private TextView recLabel;
	private TextView completedRec;
	private TextView upcomingRec;
	private TextView failedRec;

    private String connectionStatus = "";
    private String freeDiscSpace = "";
    private String totalDiscSpace = "";
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            connectionStatus  = bundle.getString(Constants.BUNDLE_CONNECTION_STATUS);
        }

        View v = inflater.inflate(R.layout.status_fragment_layout, container, false);
        connection = (TextView) v.findViewById(R.id.connection);
        status = (TextView) v.findViewById(R.id.status);
        discspaceLabel = (TextView) v.findViewById(R.id.discspace_label);
        freediscspace = (TextView) v.findViewById(R.id.free_discspace);
        totaldiscspace = (TextView) v.findViewById(R.id.total_discspace);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channels = (TextView) v.findViewById(R.id.channels);
        currentlyRecLabel = (TextView) v.findViewById(R.id.currently_recording_label);
        currentlyRec = (TextView) v.findViewById(R.id.currently_recording);
        recLabel = (TextView) v.findViewById(R.id.recording_label);
        completedRec = (TextView) v.findViewById(R.id.completed_recordings);
        upcomingRec = (TextView) v.findViewById(R.id.upcoming_recordings);
        failedRec = (TextView) v.findViewById(R.id.failed_recordings);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.status), TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        app.addListener(this);

        // Upon resume show the actual status. If the connection is OK show the
        // full status or that stuff is loading, otherwise hide certain
        // information and show the cause of the connection problem. 
        hideStatus();
        if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            onMessage(Constants.ACTION_LOADING, app.isLoading());
        } else {
            onMessage(connectionStatus, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
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
                    showConnectionStatus();
                    showChannelStatus();
                    showRecordingStatus();
                    getDiscSpaceStatus();
                }
            });
	    } else if (action.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)
	            || action.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || action.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)
                || action.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)
                || action.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
	        activity.runOnUiThread(new Runnable() {
                public void run() {
                    connectionStatus = action;
                    hideStatus();
                    showConnectionStatus();
                }
            });
        } else if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (actionBarInterface != null) {
                        final String updating = (loading ? getString(R.string.updating) : "");
                        actionBarInterface.setActionBarSubtitle(updating, TAG);
                    }

                    if (loading) {
                        status.setText(getString(R.string.loading));
                    } else {
                        showConnectionStatus();
                        showChannelStatus();
                        showRecordingStatus();
                        getDiscSpaceStatus();
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_DISC_SPACE)) {
            getActivity().runOnUiThread(new Runnable() {
                @SuppressWarnings("unchecked")
                public void run() {
                	calculateDiscSpace((Map<String, String>) obj);
                	showDiscSpaceStatus();
                }
            });
        }
	}

	    /**
     * Hides all status fields. Each field will be made visible by other methods
     * when it is required.
     */
	protected void hideStatus() {
	    discspaceLabel.setVisibility(View.GONE);
        freediscspace.setVisibility(View.GONE);
        totaldiscspace.setVisibility(View.GONE);
        channelLabel.setVisibility(View.GONE);
        channels.setVisibility(View.GONE);
        currentlyRecLabel.setVisibility(View.GONE);
        currentlyRec.setVisibility(View.GONE);
        recLabel.setVisibility(View.GONE);
        completedRec.setVisibility(View.GONE);
        upcomingRec.setVisibility(View.GONE);
        failedRec.setVisibility(View.GONE);
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
                connection.setText(getString(R.string.no_connection_available));
            } else {
                connection.setText(getString(R.string.no_connection_active));
            }
            status.setText(getString(R.string.unknown));
        } else {
            if (conn != null) {
                connection.setText(conn.name + " (" + conn.address + ")");
            }
        }

        // Show a textual description about the connection state
        if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            status.setText(getString(R.string.ready));
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)) {
            status.setText(R.string.err_con_lost);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)) {
            status.setText(R.string.err_con_timeout);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)) {
            status.setText(R.string.err_connect);
        } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
            status.setText(R.string.err_auth);
        }
    }

    /**
     * Calls the service to get the free and total disc space. Additionally the
     * loading indication for these fields are shown.
     */
    protected void getDiscSpaceStatus() {
        Intent intent = new Intent(getActivity(), HTSService.class);
        intent.setAction(Constants.ACTION_GET_DISC_STATUS);
        getActivity().startService(intent);

        discspaceLabel.setVisibility(View.VISIBLE);
        freediscspace.setVisibility(View.VISIBLE);
        totaldiscspace.setVisibility(View.VISIBLE);
        freediscspace.setText(getString(R.string.loading));
        totaldiscspace.setText(getString(R.string.loading));
    }

    /**
     * Shows the available disc space if at least one of the two fields have an
     * actual value. If both fields are empty, hide the indications and the free
     * disc space description.
     */
    protected void showDiscSpaceStatus() {
        int visible = View.VISIBLE;
        if (freeDiscSpace.length() == 0 && totalDiscSpace.length() == 0) {
            visible = View.GONE;
        }
        discspaceLabel.setVisibility(visible);
        freediscspace.setVisibility(visible);
        totaldiscspace.setVisibility(visible);
        freediscspace.setText(freeDiscSpace);
        totaldiscspace.setText(totalDiscSpace);
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     * 
     * @param obj
     */
    protected void calculateDiscSpace(Map<String, String> obj) {
        Map<String, String> list = obj;
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
     * Shows the information how many channels are available.
     */
    private void showChannelStatus() {
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        channelLabel.setVisibility(View.VISIBLE);
        channels.setVisibility(View.VISIBLE);
        channels.setText(app.getChannels().size() + " " + getString(R.string.available));
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {
        String currentRecText = "";

        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        for (Recording rec : app.getRecordings()) {
            // Add the information what is currently being recorded.
            if (rec.isRecording() == true) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                if (rec.channel != null) {
                    currentRecText += " (" + getString(R.string.channel) + " " + rec.channel.name + ")\n";
                }
            }
        }

        currentlyRecLabel.setVisibility(View.VISIBLE);
        currentlyRec.setVisibility(View.VISIBLE);
        recLabel.setVisibility(View.VISIBLE);
        completedRec.setVisibility(View.VISIBLE);
        upcomingRec.setVisibility(View.VISIBLE);
        failedRec.setVisibility(View.VISIBLE);

        // Show either the program being currently recorded or an different string
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText : getString(R.string.nothing));
        completedRec.setText(app.getRecordings(Constants.RECORDING_TYPE_COMPLETED).size() + " " + getString(R.string.completed));
        upcomingRec.setText(app.getRecordings(Constants.RECORDING_TYPE_SCHEDULED).size() + " " + getString(R.string.upcoming));
        failedRec.setText(app.getRecordings(Constants.RECORDING_TYPE_FAILED).size() + " " + getString(R.string.failed));
    }
}
