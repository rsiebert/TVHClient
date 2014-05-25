/*
 *  Copyright (C) 2013 Robert Siebert
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
package org.tvheadend.tvhclient.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.R;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RecordingListAdapter extends ArrayAdapter<Recording> {

    Activity context;
    List<Recording> list;

    public RecordingListAdapter(Activity context, List<Recording> list) {
        super(context, R.layout.recording_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Recording>() {
            public int compare(Recording x, Recording y) {
                return x.compareTo(y);
            }
        });
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView title;
        public TextView channel;
        public TextView time;
        public TextView date;
        public TextView duration;
        public TextView description;
        public TextView failed_reason;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.recording_list_widget, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.failed_reason = (TextView) view.findViewById(R.id.failed_reason);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        Recording rec = getItem(position);
        if (rec != null) {
            holder.title.setText(rec.title);
            if (holder.channel != null && rec.channel != null) {
                holder.channel.setText(rec.channel.name);
            }
            Utils.setChannelIcon(holder.icon, null, rec.channel);
            Utils.setDate(holder.date, rec.start);
            Utils.setTime(holder.time, rec.start, rec.stop);
            Utils.setDuration(holder.duration, rec.start, rec.stop);
            Utils.setDescription(null, holder.description, rec.description);
            
            // Display the reason why the recording has failed
            if (holder.failed_reason != null) {
                if (rec.error != null || 
                        (rec.state.equals("missed") || rec.state.equals("invalid"))) {
                    holder.failed_reason.setVisibility(View.VISIBLE);
                    // Show the text why it failed
                    if (rec.error != null && rec.error.equals("File missing")) {
                        holder.failed_reason.setText(R.string.recording_file_missing);
                    } else if (rec.state.equals("missed")) {
                        holder.failed_reason.setText(R.string.recording_time_missed);
                    } else if (rec.state.equals("invalid")) {
                        holder.failed_reason.setText(R.string.recording_file_invalid);
                    }
                } else {
                    holder.failed_reason.setVisibility(View.GONE);
                }
            }
        }
        return view;
    }

    public void update(Recording rec) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == rec.id) {
                list.set(i, rec);
                break;
            }
        }
    }
}