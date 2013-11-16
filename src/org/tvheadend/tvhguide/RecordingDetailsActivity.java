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
package org.tvheadend.tvhguide;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Recording;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class RecordingDetailsActivity extends ActionBarActivity implements HTSListener {

    private ActionBar actionBar = null;
    // The currently selected recording
    private Recording rec;

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
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        if (rec.channel != null) {
            actionBar.setTitle(rec.channel.name);

            // Show or hide the channel icon if required 
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon && rec.channel.iconBitmap != null) {
                actionBar.setIcon(new BitmapDrawable(getResources(), rec.channel.iconBitmap));
            }
        }

        // Initialize all the widgets from the layout
        TextView title = (TextView) findViewById(R.id.title);
        ImageView state = (ImageView) findViewById(R.id.state);
        TextView summaryLabel = (TextView) findViewById(R.id.summary_label);
        TextView summary = (TextView) findViewById(R.id.summary);
        TextView descLabel = (TextView) findViewById(R.id.description_label);
        TextView desc = (TextView) findViewById(R.id.description);
        TextView channelName = (TextView) findViewById(R.id.channel);
        TextView date = (TextView) findViewById(R.id.date);
        TextView time = (TextView) findViewById(R.id.time);
        TextView duration = (TextView) findViewById(R.id.duration);

        // Set the values
        title.setText(rec.title);
        channelName.setText(rec.channel.name);
        
        Utils.setState(state, rec);
        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);

        Utils.setDescription(summaryLabel, summary, rec.summary);
        Utils.setDescription(descLabel, desc, rec.description);
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
            Intent pi = new Intent(this, PlaybackSelectionActivity.class);
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
            
            // Update the status icon
            ImageView state = (ImageView) findViewById(R.id.state);
            Utils.setState(state, rec);
        } 
    }
}
