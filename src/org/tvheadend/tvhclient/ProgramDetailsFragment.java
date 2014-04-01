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
package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class ProgramDetailsFragment extends Fragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramDetailsFragment.class.getSimpleName();

    private Activity activity;
    private Program program;
    private Channel channel;

    private TextView title;
    private ImageView state;
    private TextView summaryLabel;
    private TextView summary;
    private TextView descLabel;
    private TextView desc;
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            // Get the channel of the program 
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            channel = app.getChannel(bundle.getLong("channelId", 0));
            long eventId = bundle.getLong("eventId", 0);
            if (channel != null) {
                // Find the program with the given id within this channel
                for (Program p : channel.epg) {
                    if (p != null && p.id == eventId) {
                        program = p;
                        break;
                    }
                }
            }
        }

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.program_layout, container, false);
        title = (TextView) v.findViewById(R.id.title);
        state = (ImageView) v.findViewById(R.id.state);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
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
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        title.setText(program.title);
        channelName.setText(channel.name);
        Utils.setState(state, program.recording);
        Utils.setDate(date, program.start);
        Utils.setTime(time, program.start, program.stop);
        Utils.setDuration(duration, program.start, program.stop);
        Utils.setProgressText(progress, program.start, program.stop);
        Utils.setDescription(summaryLabel, summary, program.summary);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.program_context_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Show or hide the menu items depending on the program state
        Utils.setProgramMenu(menu, program);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            // Show the search text input in the action bar
            activity.onSearchRequested();
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(activity, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(activity, program.title));
            return true;

        case R.id.menu_record_remove:
            Utils.removeProgram(activity, program.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(activity, program.recording.id);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(activity, program.id, program.channel.id);
            return true;

        case R.id.menu_play:
            // Open a new activity to stream the current program to this device
            Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
            intent.putExtra("channelId", program.channel.id);
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMessage(String action, Object obj) {
        // An existing program has been updated, this is valid for all menu options. 
        if (action.equals(TVHClientApplication.ACTION_PROGRAMME_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Update the options menu and the status icon
                    activity.invalidateOptionsMenu();
                    Utils.setState(state, program.recording);
                }
            });
        } 
    }
}
