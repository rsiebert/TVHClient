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

import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Date;
import java.util.Iterator;
import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Programme;

/**
 *
 * @author john-tornblom
 */
public class ChannelListViewWrapper {

    private TextView name;
    private TextView nowTitle;
    private TextView nowTime;
    private TextView nextTitle;
    private TextView nextTime;
    private ImageView icon;
    private ImageView nowProgressImage;
    private ClipDrawable nowProgress;

    public ChannelListViewWrapper(View base) {
        name = (TextView) base.findViewById(R.id.ch_name);
        nowTitle = (TextView) base.findViewById(R.id.ch_now_title);

        nowProgressImage = (ImageView) base.findViewById(R.id.ch_elapsedtime);
        nowProgress = new ClipDrawable(nowProgressImage.getDrawable(), Gravity.LEFT, ClipDrawable.HORIZONTAL);
        nowProgressImage.setBackgroundDrawable(nowProgress);

        nowTime = (TextView) base.findViewById(R.id.ch_now_time);
        nextTitle = (TextView) base.findViewById(R.id.ch_next_title);
        nextTime = (TextView) base.findViewById(R.id.ch_next_time);
        icon = (ImageView) base.findViewById(R.id.ch_icon);
    }

    public void repaint(Channel channel) {
        nowTime.setText("");
        nowTitle.setText("");
        nextTime.setText("");
        nextTitle.setText("");
        nowProgress.setLevel(0);

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
            nowTitle.setText(R.string.ch_no_transmission);
        } else if (it.hasNext()) {
            Programme p = it.next();
            nowTime.setText(
                    DateFormat.getTimeFormat(nowTime.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(nowTime.getContext()).format(p.stop));

            double duration = (p.stop.getTime() - p.start.getTime());
            double elapsed = new Date().getTime() - p.start.getTime();
            double percent = elapsed / duration;

            nowProgressImage.setVisibility(ImageView.VISIBLE);
            nowProgress.setLevel((int) Math.floor(percent * 10000));
            nowTitle.setText(p.title);
        } else {
            nowProgressImage.setVisibility(ImageView.GONE);
        }
        nowProgressImage.invalidate();
        nowTime.invalidate();
        nowTitle.invalidate();

        if (it.hasNext()) {
            Programme p = it.next();
            nextTime.setText(
                    DateFormat.getTimeFormat(nextTime.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(nextTime.getContext()).format(p.stop));

            nextTitle.setText(p.title);
        }
        nextTime.invalidate();
        nextTitle.invalidate();
    }
}
