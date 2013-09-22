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

import java.util.Date;
import java.util.Iterator;

import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;

import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 *
 * @author john-tornblom
 */
public class ChannelListViewWrapper {

    private TextView name;
    private TextView title;
    private TextView time;
    private TextView duration;
    private ImageView icon;
    private ProgressBar progress;

    public ChannelListViewWrapper(View base) {
        name = (TextView) base.findViewById(R.id.name);
        title = (TextView) base.findViewById(R.id.title);
        progress = (ProgressBar) base.findViewById(R.id.progress);
        duration = (TextView) base.findViewById(R.id.duration);
        time = (TextView) base.findViewById(R.id.time);
        icon = (ImageView) base.findViewById(R.id.icon);
    }

    public void repaint(Channel channel) {
        time.setText("");
        title.setText("");
        progress.setProgress(0);
        duration.setText("");
        name.setText(channel.name);
        name.invalidate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", false);
        icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
        icon.setBackgroundDrawable(new BitmapDrawable(channel.iconBitmap));

        if (channel.isRecording()) {
            icon.setImageResource(R.drawable.ic_rec_small);
        } else {
            icon.setImageDrawable(null);
        }
        icon.invalidate();

        Iterator<Programme> it = channel.epg.iterator();
        if (!channel.isTransmitting && it.hasNext()) {
            title.setText(R.string.ch_no_transmission);
        } else if (it.hasNext()) {
            Programme p = it.next();
            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(p.stop));

            double durationTime = (p.stop.getTime() - p.start.getTime());
            double elapsedTime = new Date().getTime() - p.start.getTime();
            double percent = elapsedTime / durationTime;

            progress.setProgress((int) Math.floor(percent * 100));
            title.setText(p.title);
            
            String durationText = "0 min";
            if (durationTime > 0)
                durationText = String.valueOf((int)durationTime / 1000 / 60) + " min";
            duration.setText(durationText);
        }

        progress.invalidate();
        time.invalidate();
        title.invalidate();
        duration.invalidate();
    }
}
