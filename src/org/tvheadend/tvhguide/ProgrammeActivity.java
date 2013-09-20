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

import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;
import org.tvheadend.tvhguide.model.SeriesInfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SparseArray;
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
public class ProgrammeActivity extends Activity {

    private Programme programme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.programme_layout);
    	
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        Channel channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null) {
            finish();
            return;
        }

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(channel.name);

        long eventId = getIntent().getLongExtra("eventId", 0);
        for (Programme p : channel.epg) {
            if (p.id == eventId) {
                programme = p;
                break;
            }
        }

        if (programme == null) {
            finish();
            return;
        }

        // Initialize all the widgets from the layout
        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        TextView title = (TextView) findViewById(R.id.title);
        TextView summaryLabel = (TextView) findViewById(R.id.summary_label);
        TextView summary = (TextView) findViewById(R.id.summary);
        TextView descLabel = (TextView) findViewById(R.id.desc_label);
        TextView desc = (TextView) findViewById(R.id.desc);
        TextView airingLabel = (TextView) findViewById(R.id.airing_label);
        TextView airingChannel = (TextView) findViewById(R.id.channel);
        TextView airing = (TextView) findViewById(R.id.airing);
        TextView contentTypeLabel = (TextView) findViewById(R.id.content_type_label);
        TextView contentType = (TextView) findViewById(R.id.content_type);
        TextView seriesInfoLabel = (TextView) findViewById(R.id.series_info_label);
        TextView seriesInfo = (TextView) findViewById(R.id.series_info);
        TextView ratingBarLabel = (TextView) findViewById(R.id.star_rating_label);
        TextView ratingBarText = (TextView) findViewById(R.id.star_rating_text);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.star_rating);
        
        // Set the values
        title.setText(programme.title);
        
        airingChannel.setText(channel.name);
        airing.setText(
                DateFormat.getLongDateFormat(airing.getContext()).format(programme.start) + "   "
                + DateFormat.getTimeFormat(airing.getContext()).format(programme.start) + " - "
                + DateFormat.getTimeFormat(airing.getContext()).format(programme.stop));

        if (programme.summary.length() == 0) { 
        	summaryLabel.setVisibility(View.GONE);
        	summary.setVisibility(View.GONE);
        } else {
            summary.setText(programme.summary);
        }
        
        if (programme.description.length() == 0) {
            descLabel.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
        } else {
            desc.setText(programme.description);
        }

        String s = buildSeriesInfoString(programme.seriesInfo);
        if (s.length() == 0) {
            seriesInfoLabel.setVisibility(View.GONE);
            seriesInfo.setVisibility(View.GONE);
        } else {
            seriesInfo.setText(s);
        }
        
        SparseArray<String> contentTypes = TVHGuideApplication.getContentTypes(this);
        s = contentTypes.get(programme.contentType, "");
        if(s.length() == 0) {
            contentTypeLabel.setVisibility(View.GONE);
            contentType.setVisibility(View.GONE);
        } else {
            contentType.setText(s);
        }
        
        if (programme.starRating < 0) {
            ratingBarLabel.setVisibility(View.GONE);
            ratingBarText.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float)programme.starRating / 10.0f);
            ratingBarText.setText("(" + programme.starRating + "/" + 100 + ")");
        }
    }

    
	public String buildSeriesInfoString(SeriesInfo info) {
		if (info.onScreen != null && info.onScreen.length() > 0)
			return info.onScreen;

		String s = "";
		String season = this.getResources().getString(string.pr_season);
		String episode = this.getResources().getString(string.pr_episode);
		String part = this.getResources().getString(string.pr_part);
		
		if(info.onScreen.length() > 0) {
			return info.onScreen;
		}
		
		if (info.seasonNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d", season.toLowerCase(), info.seasonNumber);
			if(info.seasonCount > 0)
				s += String.format("/%02d", info.seasonCount);
		}
		if (info.episodeNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %02d", episode.toLowerCase(), info.episodeNumber);
			if(info.episodeCount > 0)
				s += String.format("/%02d", info.episodeCount);
		}
		if (info.partNumber > 0) {
			if (s.length() > 0)
				s += ", ";
			s += String.format("%s %d", part.toLowerCase(), info.partNumber);
			if(info.partCount > 0)
				s += String.format("/%02d", info.partCount);
		}

		if(s.length() > 0) {
			s = s.substring(0,1).toUpperCase() + s.substring(1);
		}
		
		return s;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = null;

        if (programme.title != null) {
            item = menu.add(Menu.NONE, android.R.string.search_go, Menu.NONE, android.R.string.search_go);
            item.setIntent(new SearchEPGIntent(this, programme.title));
            item.setIcon(android.R.drawable.ic_menu_search);

            item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "IMDb");
            item.setIntent(new SearchIMDbIntent(this, programme.title));
            item.setIcon(android.R.drawable.ic_menu_info_details);
        }

        Intent intent = new Intent(this, HTSService.class);

        if (programme.recording == null) {
            intent.setAction(HTSService.ACTION_DVR_ADD);
            intent.putExtra("eventId", programme.id);
            intent.putExtra("channelId", programme.channel.id);
            item = menu.add(Menu.NONE, R.string.menu_record, Menu.NONE, R.string.menu_record);
            item.setIcon(android.R.drawable.ic_menu_save);
        } else if (programme.isRecording() || programme.isScheduled()) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            intent.putExtra("id", programme.recording.id);
            item = menu.add(Menu.NONE, R.string.menu_record_cancel, Menu.NONE, R.string.menu_record_cancel);
            item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            intent.putExtra("id", programme.recording.id);
            item = menu.add(Menu.NONE, R.string.menu_record_remove, Menu.NONE, R.string.menu_record_remove);
            item.setIcon(android.R.drawable.ic_menu_delete);
        }

        item.setIntent(intent);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean rebuild = false;
        if (programme.recording == null) {
            rebuild = menu.findItem(R.string.menu_record) == null;
        } else if (programme.isRecording() || programme.isScheduled()) {
            rebuild = menu.findItem(R.string.menu_record_cancel) == null;
        } else {
            rebuild = menu.findItem(R.string.menu_record_remove) == null;
        }

        if (rebuild) {
            menu.clear();
            return onCreateOptionsMenu(menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record_remove:
            case R.string.menu_record_cancel:
            case R.string.menu_record:
                startService(item.getIntent());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
