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
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import org.me.tvhguide.model.Recording;

/**
 *
 * @author john-tornblom
 */
public class RecordingActivity extends Activity {

    Recording rec;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
}
