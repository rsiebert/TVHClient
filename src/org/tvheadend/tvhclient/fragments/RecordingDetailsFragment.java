/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
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

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RecordingDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean isDualPane = false;
    private Recording rec;

    private TextView summaryLabel;
    private TextView summary;
    private TextView descLabel;
    private TextView desc;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView failed_reason;
    private TextView is_series_recording;
    private Toolbar toolbar;

    public static RecordingDetailsFragment newInstance(Bundle args) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        long recId = 0;
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID, 0);
            isDualPane = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }

        // Get the recording so we can show its details 
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        rec = app.getRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.recording_details_layout, container, false);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        failed_reason = (TextView) v.findViewById(R.id.failed_reason);
        is_series_recording = (TextView) v.findViewById(R.id.is_series_recording);

        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (rec != null) {
            if (getDialog() != null) {
                getDialog().setTitle(rec.title);
            }

            Utils.setDate(date, rec.start);
            Utils.setTime(time, rec.start, rec.stop);
            Utils.setDuration(duration, rec.start, rec.stop);
            Utils.setProgressText(null, rec.start, rec.stop);
            Utils.setDescription(channelLabel, channelName, ((rec.channel != null) ? rec.channel.name : ""));
            Utils.setDescription(summaryLabel, summary, rec.summary);
            Utils.setDescription(descLabel, desc, rec.description);
            Utils.setFailedReason(failed_reason, rec);

            // Show the information if the recording belongs to a series recording
            // only when no dual pane is active (the controls shall be shown)
            if (is_series_recording != null) {
                if (rec.autorecId != null && !isDualPane) {
                    is_series_recording.setVisibility(ImageView.VISIBLE);
                } else {
                    is_series_recording.setVisibility(ImageView.GONE);
                }
            }
        }

        if (toolbar != null) {
            // Set an OnMenuItemClickListener to handle menu item clicks
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
            // Inflate a menu to be displayed in the toolbar
            toolbar.inflateMenu(R.menu.recording_details_menu);
            onPrepareToolbarMenu(toolbar.getMenu());
        }
    }

    /**
     * 
     * @param menu
     */
    private void onPrepareToolbarMenu(Menu menu) {
        if (rec == null) {
            (menu.findItem(R.id.menu_play)).setVisible(false);
            (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
            (menu.findItem(R.id.menu_record_remove)).setVisible(false);

        } else if (rec.error == null && rec.state.equals("completed")) {
            // The recording is available, it can be played and removed
            (menu.findItem(R.id.menu_play)).setVisible(true);
            (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
            (menu.findItem(R.id.menu_record_remove)).setVisible(true);

        } else if (rec.isRecording() || rec.isScheduled()) {
            // The recording is recording or scheduled, it can only be cancelled
            (menu.findItem(R.id.menu_play)).setVisible(false);
            (menu.findItem(R.id.menu_record_cancel)).setVisible(true);
            (menu.findItem(R.id.menu_record_remove)).setVisible(false);

        } else if (rec.error != null || rec.state.equals("missed")) {
            // The recording has failed or has been missed, allow removal
            (menu.findItem(R.id.menu_play)).setVisible(false);
            (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
            (menu.findItem(R.id.menu_record_remove)).setVisible(true);
        }
    }

    /**
     * 
     * @param item
     * @return
     */
    protected boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_play:
            // Open a new activity that starts playing the selected recording
            if (rec != null) {
                Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
                intent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
                startActivity(intent);
            }
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, rec);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, rec);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, Object obj) {
        // An existing recording has been updated, this is valid for all menu options
        if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_DELETE)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (toolbar != null) {
                        onPrepareToolbarMenu(toolbar.getMenu());
                    }
                }
            });
        }
    }
}
