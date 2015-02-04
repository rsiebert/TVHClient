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
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.SeriesRecording;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SeriesRecordingDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = SeriesRecordingDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean isDualPane = false;
    private SeriesRecording rec = null;

    private LinearLayout detailsLayout;
    private TextView isEnabled;
    private TextView minDuration;
    private TextView maxDuration;
    private TextView retention;
    private TextView daysOfWeek;
    private TextView approxTime;
    private TextView startWindow;
    private TextView priority;
    private TextView startExtra;
    private TextView stopExtra;
    private TextView title;
    private TextView name;
    private TextView directory;
    private TextView owner;
    private TextView creator;
    private TextView channelName;

    private Toolbar toolbar;

    public static SeriesRecordingDetailsFragment newInstance(Bundle args) {
        SeriesRecordingDetailsFragment f = new SeriesRecordingDetailsFragment();
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
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        String recId = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getString(Constants.BUNDLE_SERIES_RECORDING_ID);
            isDualPane = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }

        // Get the recording so we can show its details 
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        rec = app.getSeriesRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.series_recording_details_layout, container, false);
        detailsLayout = (LinearLayout) v.findViewById(R.id.details_layout);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (TextView) v.findViewById(R.id.is_enabled);
        name = (TextView) v.findViewById(R.id.name);
        minDuration = (TextView) v.findViewById(R.id.minimum_duration);
        maxDuration = (TextView) v.findViewById(R.id.maximum_duration);
        retention = (TextView) v.findViewById(R.id.retention);
        daysOfWeek = (TextView) v.findViewById(R.id.days_of_week);
        approxTime = (TextView) v.findViewById(R.id.approx_time);
        startWindow = (TextView) v.findViewById(R.id.start_window);
        priority = (TextView) v.findViewById(R.id.priority);
        startExtra = (TextView) v.findViewById(R.id.start_extra);
        stopExtra = (TextView) v.findViewById(R.id.stop_extra);
        directory = (TextView) v.findViewById(R.id.directory);
        owner = (TextView) v.findViewById(R.id.owner);
        creator = (TextView) v.findViewById(R.id.creator);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (rec != null) {
            if (channelName != null && rec.channel != null) {
                channelName.setText(rec.channel.name);
            }
            if (title != null && rec.title.length() > 0) {
                title.setText(rec.title);
            }
            if (name != null && rec.name.length() > 0) {
                name.setText(rec.name);
            }
            if (directory != null && rec.directory.length() > 0) {
                directory.setText(rec.directory);
            }
            if (owner != null && rec.owner.length() > 0) {
                owner.setText(rec.owner);
            }
            if (creator != null && rec.creator.length() > 0) {
                creator.setText(rec.creator);
            }

            Utils.setDaysOfWeek(activity, null, daysOfWeek, rec.daysOfWeek);

            if (priority != null) {
                String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
                if (rec.priority >= 0 && rec.priority < priorityItems.length) {
                    priority.setText(priorityItems[(int) (rec.priority)]);
                }
            }

            if (minDuration != null && rec.minDuration > 0) {
                minDuration.setText(getString(R.string.minutes, (int) rec.minDuration));
            }
            if (maxDuration != null && rec.maxDuration > 0) {
                maxDuration.setText(getString(R.string.minutes, (int) rec.maxDuration));
            }
            if (retention != null) {
                retention.setText(getString(R.string.days, (int) rec.retention));
            }
            if (approxTime != null && rec.approxTime >= 0) {
                approxTime.setText(getString(R.string.minutes, rec.approxTime));
            }
            if (startWindow != null && rec.startWindow >= 0) {
                startWindow.setText(getString(R.string.minutes, rec.startWindow));
            }
            if (startExtra != null && rec.startExtra >= 0) {
                startExtra.setText(getString(R.string.minutes, (int) rec.startExtra));
            }
            if (stopExtra != null && rec.stopExtra >= 0) {
                stopExtra.setText(getString(R.string.minutes, (int) rec.stopExtra));
            }

            if (isEnabled != null) {
                if (rec.enabled) {
                    isEnabled.setText(R.string.recording_enabled);
                } else {
                    isEnabled.setText(R.string.recording_disabled);
                }
            }
        } else {
            detailsLayout.setVisibility(View.GONE);
        }

        if (toolbar != null) {
            if (rec != null && !isDualPane) {
                toolbar.setTitle(rec.title);
            }
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
            if (rec != null) {
                // Inflate a menu to be displayed in the toolbar
                toolbar.inflateMenu(R.menu.recording_details_menu);
                onPrepareToolbarMenu(toolbar.getMenu());
            }
        }
    }

    /**
     * 
     * @param menu
     */
    private void onPrepareToolbarMenu(Menu menu) {
        (menu.findItem(R.id.menu_play)).setVisible(false);
        (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
    }

    /**
     * 
     * @param item
     * @return
     */
    protected boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, rec);
            if (getDialog() != null) {
                getDialog().dismiss();
            }
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
        // NOP
    }
}
