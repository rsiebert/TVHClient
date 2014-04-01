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

import org.tvheadend.tvhclient.model.Recording;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class RecordingDetailsActivity extends ActionBarActivity {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingDetailsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private Recording rec;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);

        final long id = getIntent().getLongExtra("id", 0);
        TVHClientApplication app = (TVHClientApplication) getApplication();
        rec = app.getRecording(id);
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

        // Show the fragment
        Bundle args = new Bundle();
        args.putLong("id", id);
        Fragment fragment = Fragment.instantiate(this, RecordingDetailsFragment.class.getName());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
