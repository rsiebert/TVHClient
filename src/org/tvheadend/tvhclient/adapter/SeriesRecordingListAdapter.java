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

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        public TextView daysOfWeek;
        public TextView start;
        public TextView isEnabled;
        public ImageView dual_pane_list_item_selection;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.start = (TextView) view.findViewById(R.id.start);
            holder.isEnabled = (TextView) view.findViewById(R.id.enabled);
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
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_light : R.drawable.dual_pane_selector_dark;
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
            Utils.setDaysOfWeek(context, null, holder.daysOfWeek, srec.daysOfWeek);

            if (holder.start != null) {
                if (srec.start >= 0) {
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
                    String start = formatter.format(new Date(srec.start * 60L * 1000L));
                    holder.start.setText(start);
                } else {
                    holder.start.setText(R.string.not_set);
                }
            }

            // Show only the recording icon
            if (holder.isEnabled != null) {
                if (srec.enabled) {
                    holder.isEnabled.setText(R.string.recording_enabled);
                } else {
                    holder.isEnabled.setText(R.string.recording_disabled);
                }
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