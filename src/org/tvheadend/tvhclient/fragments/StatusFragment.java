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

import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
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
	private TextView freediscspace;
	private TextView totaldiscspace;
	private TextView channels;
	private TextView currentlyRec;
	private TextView completedRec;
	private TextView upcomingRec;
	private TextView failedRec;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.status_fragment_layout, container, false);
        connection = (TextView) v.findViewById(R.id.connection);
        status = (TextView) v.findViewById(R.id.status);
        freediscspace = (TextView) v.findViewById(R.id.free_discspace);
        totaldiscspace = (TextView) v.findViewById(R.id.total_discspace);
        channels = (TextView) v.findViewById(R.id.channels);
        currentlyRec = (TextView) v.findViewById(R.id.currently_recording);
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
        showStatus(app.isLoading());
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
	    if (action.equals(TVHClientApplication.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    showStatus(loading);
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_STATUS)) {
            getActivity().runOnUiThread(new Runnable() {
                @SuppressWarnings("unchecked")
                public void run() {
                	showDiscSpace((Map<String, String>) obj);
                }
            });
        }
	}

    /**
     * Shows the status depending if a connection is available and the status of
     * that connection.
     * 
     * @param loading
     */
    protected void showStatus(boolean loading) {

        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.status), TAG);
        }
        // Get the name of the current connection
        Connection conn = null;
        if (DatabaseHelper.getInstance() != null) {
            conn = DatabaseHelper.getInstance().getSelectedConnection();
        }

        // We are not connected or loading, show the defaults
        if (conn == null || loading) {
            connection.setText(getString(R.string.unknown));
            status.setText(getString(R.string.unknown));
            freediscspace.setText(getString(R.string.unknown));
            totaldiscspace.setText(getString(R.string.unknown));
            channels.setText(getString(R.string.unknown));
            currentlyRec.setText(getString(R.string.unknown));
            completedRec.setText(getString(R.string.unknown));
            upcomingRec.setText(getString(R.string.unknown));
            failedRec.setText(getString(R.string.unknown));
        } else {
            // Show the connection name and the loading status
            if (conn != null) {
                connection.setText(conn.name + " (" + conn.address + ")");
    
                if (loading) {
                    status.setText(getString(R.string.loading));
                } else {
                    status.setText(getString(R.string.ready));
                    showChannelStatus();
                    showRecordingStatus();
        
                    // Upon the first call no information is available about the disc space
                    // so call the server to get that information.
                    Intent intent = new Intent(getActivity(), HTSService.class);
                    intent.setAction(HTSService.ACTION_GET_DISC_STATUS);
                    getActivity().startService(intent);
                }
            }
        }
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     * 
     * @param obj
     */
    protected void showDiscSpace(Map<String, String> obj) {
        Map<String, String> list = obj;
        try {
            // Get the disc space values and convert them to megabytes
            long free = (Long.parseLong(list.get("freediskspace")) / 1000000);
            long total = (Long.parseLong(list.get("totaldiskspace")) / 1000000);

            // Show the free amount of disc space as GB or MB
            if (free > 1000) {
                freediscspace.setText((free / 1000) + " GB " + getString(R.string.available));
            } else {
                freediscspace.setText(free + " MB " + getString(R.string.available));
            }
            // Show the total amount of disc space as GB or MB
            if (total > 1000) {
                totaldiscspace.setText((total / 1000) + " GB " + getString(R.string.total));
            } else {
                totaldiscspace.setText(total + " MB " + getString(R.string.total));
            }
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
        channels.setText(app.getChannels().size() + " " + getString(R.string.available));
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {
        int completedRecCount = 0;
        int upcomingRecCount = 0;
        int failedRecCount = 0;
        String currentRecText = "";
        
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        for (Recording rec : app.getRecordings()) {
            if (rec.error == null && rec.state.equals("completed")) {
                ++completedRecCount;
            } else if (rec.error == null &&
                    (rec.state.equals("scheduled") || 
                     rec.state.equals("recording") ||
                     rec.state.equals("autorec"))) {
                ++upcomingRecCount;
            } else if ((rec.error != null || 
                    (rec.state.equals("missed") || 
                            rec.state.equals("invalid")))) {
                ++failedRecCount;
            }
            
            // Add the information what is currently being recorded.
            if (rec.isRecording() == true) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                if (rec.channel != null) {
                    currentRecText += " (" + getString(R.string.channel) + " " + rec.channel.name + ")\n";
                }
            }
        }

        // Show either the program being currently recorded or an different string
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText : getString(R.string.nothing));
        completedRec.setText(completedRecCount + " " + getString(R.string.completed));
        upcomingRec.setText(upcomingRecCount + " " + getString(R.string.upcoming));
        failedRec.setText(failedRecCount + " " + getString(R.string.failed));
    }
}
