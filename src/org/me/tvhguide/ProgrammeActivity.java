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
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.ViewFlipper;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Programme;

/**
 *
 * @author john-tornblom
 */
public class ProgrammeActivity extends Activity {

    ViewFlipper vf;
    Channel channel;
    float oldTouchValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.pr_flipper);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.pr_title);

        vf = (ViewFlipper) findViewById(R.id.pr_switcher);
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        long id = getIntent().getLongExtra("channelId", 0);

        for (Channel ch : app.getChannels()) {
            if (ch.id == id) {
                channel = ch;
            }
        }

        if (channel == null) {
            return;
        }

        TextView text = (TextView) findViewById(R.id.pr_title);
        text.setText(channel.name);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int count = 0;
        for (Programme p : channel.epg) {
            View view = inflater.inflate(R.layout.pr_widget, null);

            text = (TextView) view.findViewById(R.id.pr_name);
            text.setText(p.title);

            text = (TextView) view.findViewById(R.id.pr_desc);
            text.setText(p.description);

            text = (TextView) view.findViewById(R.id.pr_time);
            text.setText(DateFormat.getTimeFormat(this).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(this).format(p.stop));

            vf.addView(view);
            count++;
        }

        text = (TextView) findViewById(R.id.pr_count);
        text.setText("1/" + count);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        View currentView = vf.getCurrentView();

        if (currentView == null) {
            return false;
        }

        float diff = event.getX() - oldTouchValue;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldTouchValue = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                currentView.layout((int) (diff),
                        currentView.getTop(), currentView.getRight(),
                        currentView.getBottom());

                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(diff / currentView.getRight()) < 0.25) {

                    currentView.layout((int) 0,
                            currentView.getTop(), currentView.getRight(),
                            currentView.getBottom());

                    break;
                }

                if (diff < 0) {
                    vf.showNext();
                } else {
                    vf.showPrevious();
                }
                break;
        }

        return true;
    }
}
