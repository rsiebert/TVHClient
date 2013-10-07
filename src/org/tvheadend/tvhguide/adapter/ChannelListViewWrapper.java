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
package org.tvheadend.tvhguide.adapter;

import java.util.Date;
import java.util.Iterator;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.Utils;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Program;

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
    private ImageView state;
    private ProgressBar progress;

    public ChannelListViewWrapper(View base) {
        name = (TextView) base.findViewById(R.id.channel);
        title = (TextView) base.findViewById(R.id.title);
        progress = (ProgressBar) base.findViewById(R.id.progress);
        duration = (TextView) base.findViewById(R.id.duration);
        time = (TextView) base.findViewById(R.id.time);
        icon = (ImageView) base.findViewById(R.id.icon);
        state = (ImageView) base.findViewById(R.id.state);
    }

    public void repaint(Channel channel) {
        
        // Set the initial values
        progress.setProgress(0);
        name.setText(channel.name);
        Utils.setChannelIcon(icon, null, channel);

        // Add a small recording icon above the channel icon, if we are
        // recording the current program.
        if (channel.isRecording()) {
            state.setImageResource(R.drawable.ic_rec_small);
            state.setVisibility(View.VISIBLE);
        } else {
            state.setImageDrawable(null);
            state.setVisibility(View.GONE);
        }
        
        // Get the iterator so we can check the channel status 
        Iterator<Program> it = channel.epg.iterator();
        
        // Check if the channel is actually transmitting 
        // data and contains program data which can be shown.
        if (!channel.isTransmitting && it.hasNext()) {
            title.setText(R.string.ch_no_transmission);
        } else if (it.hasNext()) {
            
            // Get the program that is currently running
            // and set all the available values
            Program p = it.next();
            title.setText(p.title);
            Utils.setTime(time, p.start, p.stop);
            Utils.setDuration(duration, p.start, p.stop);

            // Get the start and end times to calculate the progress.
            double durationTime = (p.stop.getTime() - p.start.getTime());
            double elapsedTime = new Date().getTime() - p.start.getTime();
            
            // Show the progress as a percentage
            double percent = 0;
            if (durationTime > 0)
                percent = elapsedTime / durationTime;
            progress.setProgress((int) Math.floor(percent * 100));
            progress.setVisibility(View.VISIBLE);

        } else {
            // The channel does not provide program data. Hide the progress bar
            // and clear the time and duration texts. These two items provide 
            // some space so that the next list item is not too close.
            title.setText(R.string.ch_no_data);
            progress.setVisibility(View.GONE);
        }

        // Invalidate the views that have been set above
        icon.invalidate();
        name.invalidate();
        progress.invalidate();
        title.invalidate();
    }
}
