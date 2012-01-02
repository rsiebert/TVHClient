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
package org.me.tvhguide;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import java.net.URLEncoder;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.intent.SearchEPGIntent;
import org.me.tvhguide.intent.SearchIMDbIntent;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Programme;

/**
 *
 * @author john-tornblom
 */
public class ProgrammeActivity extends Activity {

    private Programme programme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        Channel channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null) {
            finish();
            return;
        }

        long eventId = getIntent().getLongExtra("eventId", 0);
        for (Programme p : channel.epg) {
            if (p.id == eventId) {
                programme = p;
            }
        }

        if (programme == null) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        setContentView(R.layout.programme_layout);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.programme_title);
        TextView t = (TextView) findViewById(R.id.ct_title);
        t.setText(channel.name);

        if (channel.iconBitmap != null) {
            ImageView iv = (ImageView) findViewById(R.id.ct_logo);
            iv.setImageBitmap(channel.iconBitmap);
        }

        TextView text = (TextView) findViewById(R.id.pr_title);
        text.setText(programme.title);

        text = (TextView) findViewById(R.id.pr_time);
        text.setText(
                DateFormat.getLongDateFormat(text.getContext()).format(programme.start)
                + "   "
                + DateFormat.getTimeFormat(text.getContext()).format(programme.start)
                + " - "
                + DateFormat.getTimeFormat(text.getContext()).format(programme.stop));

        text = (TextView) findViewById(R.id.pr_desc);
        text.setText(programme.ext_desc);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem item = null;
        Intent intent = null;

        item = menu.add(Menu.NONE, android.R.string.search_go, Menu.NONE, android.R.string.search_go);
        item.setIntent(new SearchEPGIntent(this, programme.title));
        item.setIcon(android.R.drawable.ic_menu_search);

        item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "IMDb");
        item.setIntent(new SearchIMDbIntent(this, programme.title));
        item.setIcon(android.R.drawable.ic_menu_info_details);

        intent = new Intent(this, HTSService.class);

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
