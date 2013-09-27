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

import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Recording;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * 
 * @author john-tornblom
 */
public class RecordingActivity extends Activity {

    Recording rec;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Apply the specified theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? android.R.style.Theme_Holo_Light : android.R.style.Theme_Holo);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = null;

        if (rec.title != null) {
            item = menu.add(Menu.NONE, android.R.string.search_go, Menu.NONE, android.R.string.search_go);
            item.setIntent(new SearchEPGIntent(this, rec.title));
            item.setIcon(android.R.drawable.ic_menu_search);

            item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "IMDb");
            item.setIntent(new SearchIMDbIntent(this, rec.title));
            item.setIcon(android.R.drawable.ic_menu_info_details);
        }

        Intent intent = new Intent(this, HTSService.class);

        if (rec.isRecording() || rec.isScheduled()) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            intent.putExtra("id", rec.id);
            item = menu.add(Menu.NONE, R.string.menu_record_cancel, Menu.NONE, R.string.menu_record_cancel);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            item.setIntent(intent);
        }
        else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            intent.putExtra("id", rec.id);
            item = menu.add(Menu.NONE, R.string.menu_record_remove, Menu.NONE, R.string.menu_record_remove);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_delete);
            item.setIntent(intent);

            intent = new Intent(this, ExternalPlaybackActivity.class);
            intent.putExtra("dvrId", rec.id);
            item = menu.add(Menu.NONE, R.string.ch_play, Menu.NONE, R.string.ch_play);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_view);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean rebuild = false;
        if (rec.isRecording() || rec.isScheduled()) {
            rebuild = menu.findItem(R.string.menu_record_cancel) == null;
        }
        else {
            rebuild = menu.findItem(R.string.menu_record_remove) == null;
        }

        if (rebuild) {
            menu.clear();
            return onCreateOptionsMenu(menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.string.menu_record_remove: {
            new AlertDialog.Builder(this).setTitle(R.string.menu_record_remove)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            startService(item.getIntent());
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // NOP
                        }
                    }).show();
        }
        case R.string.menu_record_cancel:
            startService(item.getIntent());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
