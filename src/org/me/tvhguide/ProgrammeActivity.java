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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Window;
import android.widget.TextView;
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

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.pr_layout);

        setTitle(channel.name);
        if (channel.iconBitmap == null) {
            setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo_72);
        } else {
            setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(channel.iconBitmap));
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
}
