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
        
        // Set some default values
        time.setText("");
        title.setText("");
        progress.setProgress(0);
        duration.setText("");
        name.setText(channel.name);
        
        // Show the icons if the user has activated this in the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
        Boolean showIcons = prefs.getBoolean("showIconPref", false);
        icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
        icon.setBackground(new BitmapDrawable(icon.getResources(), channel.iconBitmap));

        // Add a small recording icon above the channel icon, if we are
        // recording the current program.
        if (channel.isRecording()) {
            icon.setImageResource(R.drawable.ic_rec_small);
        } else {
            icon.setImageDrawable(null);
        }
        
        // Get the iterator so we can check the channel status 
        Iterator<Programme> it = channel.epg.iterator();
        
        // Check if the channel is actually transmitting 
        // data and contains program data which can be shown.
        if (!channel.isTransmitting && it.hasNext()) {
            title.setText(R.string.ch_no_transmission);
        } else if (it.hasNext()) {
            Programme p = it.next();
            title.setText(p.title);
            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(p.stop));

            // Get the start and end times so we can show them 
            // and calculate the duration and current shown progress.
            double durationTime = (p.stop.getTime() - p.start.getTime());
            double elapsedTime = new Date().getTime() - p.start.getTime();
            
            // Show the progress as a percentage
            double percent = 0;
            if (durationTime > 0)
                percent = elapsedTime / durationTime;
            progress.setProgress((int) Math.floor(percent * 100));
            progress.setVisibility(View.VISIBLE);
            
            // Show the duration in minutes
            durationTime = (durationTime / 1000 / 60);
            duration.setText(duration.getContext().getString(R.string.ch_minutes, (int)durationTime));
            
        } else {
            // The channel does not provide program data. Hide the progress bar
            // and clear the time and duration texts. These two items provide 
            // some space so that the next list item is not too close.
            title.setText(R.string.ch_no_data);
            progress.setVisibility(View.GONE);
        }

        icon.invalidate();
        name.invalidate();
        progress.invalidate();
        time.invalidate();
        title.invalidate();
        duration.invalidate();
    }
}
