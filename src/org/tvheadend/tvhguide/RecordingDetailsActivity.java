/*
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
package org.tvheadend.tvhguide;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * 
 * @author john-tornblom
 */
public class RecordingDetailsActivity extends Activity implements HTSListener {

 // The currently selected recording
    Recording rec;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Apply the specified theme
        setTheme(Utils.getThemeId(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_layout);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        rec = app.getRecording(getIntent().getLongExtra("id", 0));
        if (rec == null) {
            finish();
            return;
        }

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (rec.channel != null) {
            getActionBar().setTitle(rec.channel.name);
            if (rec.channel.iconBitmap != null) {
                getActionBar().setIcon(new BitmapDrawable(getResources(), rec.channel.iconBitmap));
            }
        }

        // Initialize all the widgets from the layout
        TextView title = (TextView) findViewById(R.id.title);
        TextView summaryLabel = (TextView) findViewById(R.id.summary_label);
        TextView summary = (TextView) findViewById(R.id.summary);
        TextView descLabel = (TextView) findViewById(R.id.desc_label);
        TextView desc = (TextView) findViewById(R.id.desc);
        TextView channelName = (TextView) findViewById(R.id.channel);
        TextView date = (TextView) findViewById(R.id.date);
        TextView time = (TextView) findViewById(R.id.time);
        TextView duration = (TextView) findViewById(R.id.duration);

        // Set the values
        title.setText(rec.title);

        // Set the date, the channel name, the time and duration
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        date.setText(sdf.format(rec.start.getTime()));
        channelName.setText(rec.channel.name);

        time.setText(DateFormat.getTimeFormat(time.getContext()).format(rec.start) + " - "
                + DateFormat.getTimeFormat(time.getContext()).format(rec.stop));
        
        // Get the start and end times so we can show them 
        // and calculate the duration. Then show the duration in minutes
        double durationTime = (rec.stop.getTime() - rec.start.getTime());
        durationTime = (durationTime / 1000 / 60);
        if (durationTime > 0) {
            duration.setText("(" + duration.getContext().getString(R.string.ch_minutes, (int)durationTime) + ")");
        } else {
            duration.setVisibility(View.GONE);
        }

        // Show the program summary if it exists and only of no description is available 
        if (rec.summary.length() == 0 && rec.description.length() > 0) { 
            summaryLabel.setVisibility(View.GONE);
            summary.setVisibility(View.GONE);
        } else {
            summary.setText(rec.summary);
        }
        
        // Show the description to the program
        if (rec.description.length() == 0) {
            descLabel.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
        } else {
            desc.setText(rec.description);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Show or hide the menu items depending on the recording state
        Utils.setRecordingMenu(menu, rec);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;

        case R.id.menu_search:
            // Show the search text input in the action bar
            onSearchRequested();
            return true;

        case R.id.menu_search_imdb:
            startActivity(new SearchIMDbIntent(this, rec.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(this, rec.title));
            return true;

        case R.id.menu_record_remove:
            Utils.removeProgram(this, rec.id);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(this, rec.id);
            return true;

        case R.id.menu_play:
            Intent pi = new Intent(this, ExternalPlaybackActivity.class);
            pi.putExtra("dvrId", rec.id);
            startActivity(pi);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMessage(String action, Object obj) {
        // An existing program has been updated, this is valid for all menu options. 
        if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_UPDATE)) {
            invalidateOptionsMenu();
        } 
    }
}
