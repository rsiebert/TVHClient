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

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.SeriesRecording;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SeriesRecordingListAdapter extends ArrayAdapter<SeriesRecording> {

    Activity context;
    List<SeriesRecording> list;
    private int selectedPosition = 0;
    private int layout;

    public SeriesRecordingListAdapter(Activity context, List<SeriesRecording> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }

    public void sort(final int type) {
        switch (type) {
        case Constants.RECORDING_SORT_ASCENDING:
            sort(new Comparator<SeriesRecording>() {
                public int compare(SeriesRecording x, SeriesRecording y) {
                    return (y.title.compareTo(x.title));
                }
            });
        break;
        case Constants.RECORDING_SORT_DESCENDING:
            sort(new Comparator<SeriesRecording>() {
                public int compare(SeriesRecording x, SeriesRecording y) {
                    return (x.title.compareTo(y.title));
                }
            });
            break;
        }
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView title;
        public TextView channel;
        public ImageView state;
        public TextView daysOfWeek;
        public TextView approxTime;
        public TextView description;
        public ImageView dual_pane_list_item_selection;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.state = (ImageView) view.findViewById(R.id.state);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.approxTime = (TextView) view.findViewById(R.id.approxTime);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.dual_pane_list_item_selection = (ImageView) view.findViewById(R.id.dual_pane_list_item_selection);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (holder.dual_pane_list_item_selection != null) {
            // Set the correct indication when the dual pane mode is active
            // If the item is selected the the arrow will be shown, otherwise
            // only a vertical separation line is displayed.                
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean lightTheme = prefs.getBoolean("lightThemePref", true);

            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            }
        }

        // Get the program and assign all the values
        SeriesRecording srec = getItem(position);
        if (srec != null) {
            holder.title.setText(srec.title);
            if (holder.channel != null && srec.channel != null) {
                holder.channel.setText(srec.channel.name);
            }
            Utils.setChannelIcon(holder.icon, null, srec.channel);
            Utils.setDescription(null, holder.description, srec.description);

            // Hide this for now
            if (holder.daysOfWeek != null) {
                holder.daysOfWeek.setVisibility(View.GONE);
            }
            // Hide this for now
            if (holder.approxTime != null) {
                holder.approxTime.setVisibility(View.GONE);
            }

            // Show only the recording icon
            if (holder.state != null) {
                holder.state.setVisibility(View.GONE);
//                if (srec.enabled) {
//                    holder.state.setImageResource(R.drawable.ic_enabled_small);
//                } else {
//                    holder.state.setImageResource(R.drawable.ic_disabled_small);
//                }
            }
        }
        return view;
    }

    public void update(SeriesRecording srec) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id.compareTo(srec.id) == 0) {
                list.set(i, srec);
                break;
            }
        }
    }

    public SeriesRecording getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}