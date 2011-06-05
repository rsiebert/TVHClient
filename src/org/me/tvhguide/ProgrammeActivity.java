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
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import org.me.tvhguide.htsp.HTSService;
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

        setContentView(R.layout.pr_layout);

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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pr_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_record: {
                Intent intent = new Intent(ProgrammeActivity.this, HTSService.class);
                intent.setAction(HTSService.ACTION_DVR_ADD);
                intent.putExtra("eventId", programme.id);
                startService(intent);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}
