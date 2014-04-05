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
import java.util.Iterator;
import java.util.List;

import org.tvheadend.tvhclient.ProgramListActivity;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.R;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChannelListAdapter extends ArrayAdapter<Channel> {

    private Activity context;
    private List<Channel> list;
    private int layout;

    public ChannelListAdapter(Activity context, List<Channel> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Channel>() {
            public int compare(Channel x, Channel y) {
                return x.compareTo(y);
            }
        });
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView icon_text;
        public TextView title;
        public TextView channel;
        public TextView time;
        public TextView duration;
        public ProgressBar progress;
        public ImageView state;
        public TextView genre;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.icon_text = (TextView) view.findViewById(R.id.icon_text);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.progress = (ProgressBar) view.findViewById(R.id.progress);
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.duration = (TextView) view.findViewById(R.id.duration);
            holder.state = (ImageView) view.findViewById(R.id.state);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        final Channel c = getItem(position);
        if (c != null) {

            // Set the initial values
            if (holder.progress != null) {
                holder.progress.setProgress(0);
            }
            if (holder.channel != null) {
                holder.channel.setText(c.name);
            }
            if (holder.icon != null) {
                Utils.setChannelIcon(holder.icon, holder.icon_text, null, c);

                // Add the listener to the icon so that a 
                // click calls the program list of this channel
                holder.icon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (c.epg.isEmpty()) {
                            return;
                        }
                        Intent intent = new Intent(context, ProgramListActivity.class);
                        intent.putExtra("channelId", c.id);
                        context.startActivity(intent);
                    }
                });
            }

            // Add a small recording icon above the channel icon, if we are
            // recording the current program.
            if (holder.state != null) {
                if (c.isRecording()) {
                    holder.state.setImageResource(R.drawable.ic_rec_small);
                    holder.state.setVisibility(View.VISIBLE);
                } else {
                    holder.state.setImageDrawable(null);
                    holder.state.setVisibility(View.GONE);
                }
            }

            // Get the iterator so we can check the channel status 
            Iterator<Program> it = c.epg.iterator();
            
            // Check if the channel is actually transmitting
            // data and contains program data which can be shown.
            if (!c.isTransmitting && it.hasNext()) {
                if (holder.title != null) {
                    holder.title.setText(R.string.no_transmission);
                }
            } else if (it.hasNext()) {
                // Get the program that is currently running
                // and set all the available values
                Program p = it.next();
                if (holder.title != null) {
                    holder.title.setText(p.title);
                }
                Utils.setTime(holder.time, p.start, p.stop);
                Utils.setDuration(holder.duration, p.start, p.stop);
                Utils.setProgress(holder.progress, p.start, p.stop);
                Utils.setGenreColor(context, holder.genre, p.contentType);
            }
            else {
                // The channel does not provide program data. Hide the progress
                // bar,the time and duration texts.
                if (holder.title != null) {
                    holder.title.setText(R.string.no_data);
                }
                if (holder.progress != null) {
                    holder.progress.setVisibility(View.GONE);
                }
                if (holder.time != null) {
                    holder.time.setVisibility(View.GONE);
                }
                if (holder.duration != null) {
                    holder.duration.setVisibility(View.GONE);
                }
            }
        }
        return view;
    }

    public void update(Channel c) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == c.id) {
                list.set(i, c);
                break;
            }
        }
    }
}
