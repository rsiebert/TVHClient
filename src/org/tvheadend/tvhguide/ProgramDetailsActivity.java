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

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 *
 * @author john-tornblom
 */
public class ProgramDetailsActivity extends Activity implements HTSListener {

    // The currently selected program
    private Program program;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.program_layout);
    	
    	// Get the channel which holds the program
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        Channel channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null) {
            finish();
            return;
        }

        // Get the selected program from the current channel
        long eventId = getIntent().getLongExtra("eventId", 0);
        for (Program p : channel.epg) {
            if (p.id == eventId) {
                program = p;
                break;
            }
        }

        if (program == null) {
            finish();
            return;
        }

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(channel.name);

        // Show or hide the channel icon if required
        boolean showIcon = Utils.showChannelIcons(this);
        getActionBar().setDisplayUseLogoEnabled(showIcon);
        if (showIcon)
            getActionBar().setIcon(new BitmapDrawable(getResources(), channel.iconBitmap));
        
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
        TextView contentTypeLabel = (TextView) findViewById(R.id.content_type_label);
        TextView contentType = (TextView) findViewById(R.id.content_type);
        TextView seriesInfoLabel = (TextView) findViewById(R.id.series_info_label);
        TextView seriesInfo = (TextView) findViewById(R.id.series_info);
        TextView ratingBarLabel = (TextView) findViewById(R.id.star_rating_label);
        TextView ratingBarText = (TextView) findViewById(R.id.star_rating_text);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.star_rating);
        
        // Set the values
        title.setText(program.title);
        channelName.setText(channel.name);
        
        Utils.setState(state, program.recording);
        Utils.setDate(date, program.start);
        Utils.setTime(time, program.start, program.stop);
        Utils.setDuration(duration, program.start, program.stop);

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
        // Show or hide the menu items depending on the program state
        Utils.setProgramMenu(menu, program);
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
            startActivity(new SearchIMDbIntent(this, program.title));
            return true;

        case R.id.menu_search_epg:
            startActivity(new SearchEPGIntent(this, program.title));
            return true;

        case R.id.menu_record_remove:
            Utils.removeProgram(this, program.recording.id);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(this, program.recording.id);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(this, program.id, program.channel.id);
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
