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
package org.tvheadend.tvhguide;

import java.util.Map;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Recording;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusFragment extends Fragment implements HTSListener {

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
        if (container == null)
            return null;

        View v = inflater.inflate(R.layout.status_layout, container, false);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
	}
	
    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);

        // Get the information about the disc space
        Intent intent = new Intent(getActivity(), HTSService.class);
        intent.setAction(HTSService.ACTION_GET_DISC_STATUS);
        getActivity().startService(intent);
        
        // Show the channel status
        showChannelSatus();
        
        // Show the information about the recordings
        showRecordingStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

	@Override
	public void onMessage(final String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_STATUS)) {
            getActivity().runOnUiThread(new Runnable() {
                @SuppressWarnings("unchecked")
                public void run() {
                	showDiscSpace((Map<String, String>) obj);
                }
            });
        }
	}

    protected void showDiscSpace(Map<String, String> obj) {
        Map<String, String> list = obj;
        try {
            // Get the disc space values and convert them to megabytes
            long free = (Long.parseLong(list.get("freediskspace")) / 1000000);
            long total = (Long.parseLong(list.get("totaldiskspace")) / 1000000);

            // Show the free amount of disc space as GB or MB
            if (free > 1000)
                freediscspace.setText((free / 1000) + " GB " + getString(R.string.available));
            else
                freediscspace.setText(free + " MB " + getString(R.string.available));
            
            // Show the total amount of disc space as GB or MB
            if (total > 1000)
                totaldiscspace.setText((total / 1000) + " GB " + getString(R.string.total));
            else
                totaldiscspace.setText(total + " MB " + getString(R.string.total));
        }
        catch (Exception e) {
            // Set the default values
            freediscspace.setText(getString(R.string.unknown));
            totaldiscspace.setText(getString(R.string.unknown));
        }
    }

    private void showChannelSatus() {
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        channels.setText(app.getChannels().size() + " " + getString(R.string.available));
    }

    private void showRecordingStatus() {
        
        int completedRecCount = 0;
        int upcomingRecCount = 0;
        int failedRecCount = 0;
        
        // Initialize an empty string
        String currentRecText = "";
        
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        
        for (Recording rec : app.getRecordings()) {
            if (rec.error == null && rec.state.equals("completed")) {
                ++completedRecCount;
            }
            else if (rec.error == null &&
                    (rec.state.equals("scheduled") || 
                     rec.state.equals("recording") ||
                     rec.state.equals("autorec"))) {
                ++upcomingRecCount;
            }
            else if ((rec.error != null || 
                    (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                ++failedRecCount;
            }
            
            // Add the information what is currently being recorded.
            if (rec.isRecording() == true) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                if (rec.channel != null)
                    currentRecText += " (" + getString(R.string.channel) + " " + rec.channel.name + ")\n";
            }
        }

        // Show either the program being currently recorded or an different string
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText : getString(R.string.nothing));
        completedRec.setText(completedRecCount + " " + getString(R.string.completed));
        upcomingRec.setText(upcomingRecCount + " " + getString(R.string.upcoming));
        failedRec.setText(failedRecCount + " " + getString(R.string.failed));
    }
}
