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

import java.util.Date;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

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
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class ProgramDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramDetailsFragment.class.getSimpleName();

    private Activity activity;
    private Program program;
    private Channel channel;

    private ImageView state;
    private TextView summaryLabel;
    private TextView summary;
    private TextView descLabel;
    private TextView desc;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView progress;
    private TextView contentTypeLabel;
    private TextView contentType;
    private TextView seriesInfoLabel;
    private TextView seriesInfo;
    private TextView ratingBarLabel;
    private TextView ratingBarText;
    private RatingBar ratingBar;
    private Toolbar toolbar;
    
    public static ProgramDetailsFragment newInstance(Bundle args) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
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

        long channelId = 0;
        long programId = 0;

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelId = bundle.getLong(Constants.BUNDLE_CHANNEL_ID, 0);
            programId = bundle.getLong(Constants.BUNDLE_PROGRAM_ID, 0);
        }
        
        // Get the channel of the program
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        channel = app.getChannel(channelId);
        if (channel != null) {
            // Find the program with the given id within this channel so we can
            // show the program details
            for (Program p : channel.epg) {
                if (p != null && p.id == programId) {
                    program = p;
                    break;
                }
            }
        }

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.program_details_layout, container, false);
        state = (ImageView) v.findViewById(R.id.state);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        progress = (TextView) v.findViewById(R.id.progress);
        contentTypeLabel = (TextView) v.findViewById(R.id.content_type_label);
        contentType = (TextView) v.findViewById(R.id.content_type);
        seriesInfoLabel = (TextView) v.findViewById(R.id.series_info_label);
        seriesInfo = (TextView) v.findViewById(R.id.series_info);
        ratingBarLabel = (TextView) v.findViewById(R.id.star_rating_label);
        ratingBarText = (TextView) v.findViewById(R.id.star_rating_text);
        ratingBar = (RatingBar) v.findViewById(R.id.star_rating);

        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the channel or program is null exit
        if (channel == null || program == null) {
            return;
        }

        // Show the program information
        Utils.setState(activity, state, program);
        Utils.setDate(date, program.start);
        Utils.setTime(time, program.start, program.stop);
        Utils.setDuration(duration, program.start, program.stop);
        Utils.setProgressText(progress, program.start, program.stop);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setDescription(summaryLabel, summary, program.summary);
        Utils.setDescription(channelLabel, channelName, channel.name);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setSeriesInfo(seriesInfoLabel, seriesInfo, program.seriesInfo);
        Utils.setContentType(contentTypeLabel, contentType, program.contentType);
        
        // Show the rating information as starts
        if (program.starRating < 0) {
            ratingBarLabel.setVisibility(View.GONE);
            ratingBarText.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float)program.starRating / 10.0f);
            ratingBarText.setText("(" + program.starRating + "/" + 100 + ")");
        }
        
        if (toolbar != null) {
            if (program != null) {
                toolbar.setTitle(program.title);
            }
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
            // Inflate a menu to be displayed in the toolbar
            toolbar.inflateMenu(R.menu.program_details_menu);
            onPrepareToolbarMenu(toolbar.getMenu());
        }
    }

    /**
     * 
     * @param menu
     */
    private void onPrepareToolbarMenu(Menu menu) {
        if (program == null) {
            return;
        }
        // Show the play menu item when the current 
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (program.start != null && program.stop != null
                && currentTime > program.start.getTime()
                && currentTime < program.stop.getTime()) {
            (menu.findItem(R.id.menu_play)).setVisible(true);
        } else {
            (menu.findItem(R.id.menu_play)).setVisible(false);
        }
        
        if (program.recording == null) {
            // Show the cancel menu
            (menu.findItem(R.id.menu_record_cancel)).setVisible(false);

        } else if (program.isRecording() || program.isScheduled()) {
            // Show the cancel and play menu
            (menu.findItem(R.id.menu_record_once)).setVisible(false);
            (menu.findItem(R.id.menu_record_series)).setVisible(false);

        } else {
            (menu.findItem(R.id.menu_record_once)).setVisible(false);
            (menu.findItem(R.id.menu_record_series)).setVisible(false);
            (menu.findItem(R.id.menu_record_cancel)).setVisible(false);
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
            // Open a new activity that starts playing the selected program
            if (program != null && program.channel != null) {
                Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
                startActivity(intent);
            }
            return true;

        case R.id.menu_record_once:
            Utils.recordProgram(activity, program, false);
            return true;

        case R.id.menu_record_series:
            Utils.recordProgram(activity, program, true);
            return true;
            
        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, program.recording);
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

    @Override
    public void onMessage(String action, Object obj) {
        // An existing program has been updated, this is valid for all menu options. 
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

    