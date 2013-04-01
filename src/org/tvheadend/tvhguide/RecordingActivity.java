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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Recording;

/**
 *
 * @author john-tornblom
 */
public class RecordingActivity extends Activity {

    Recording rec;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);

        super.onCreate(savedInstanceState);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        rec = app.getRecording(getIntent().getLongExtra("id", 0));
        if (rec == null) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        setContentView(R.layout.recording_layout);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.recording_title);
        TextView t = (TextView) findViewById(R.id.ct_title);
        t.setText(rec.channel.name);

        if (rec.channel.iconBitmap != null) {
            ImageView iv = (ImageView) findViewById(R.id.ct_logo);
            iv.setImageBitmap(rec.channel.iconBitmap);
        }

        TextView text = (TextView) findViewById(R.id.rec_name);
        text.setText(rec.title);

        text = (TextView) findViewById(R.id.rec_summary);
        text.setText(rec.summary);
        if(rec.summary.length() == 0)
        	text.setVisibility(TextView.GONE);
        
        text = (TextView) findViewById(R.id.rec_desc);
        text.setText(rec.description);

        text = (TextView) findViewById(R.id.rec_time);
        text.setText(
                DateFormat.getLongDateFormat(this).format(rec.start)
                + "   "
                + DateFormat.getTimeFormat(this).format(rec.start)
                + " - "
                + DateFormat.getTimeFormat(this).format(rec.stop));
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
        } else {
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record_remove: {
                new AlertDialog.Builder(this)
                .setTitle(R.string.menu_record_remove)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        startService(item.getIntent());
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //NOP
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
