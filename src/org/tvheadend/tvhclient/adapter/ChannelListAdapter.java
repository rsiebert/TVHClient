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
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChannelListAdapter extends ArrayAdapter<Channel> {

    private final static String TAG = ChannelListAdapter.class.getSimpleName();

    private Activity context;
    private List<Channel> list;
    private int layout;
    private int selectedPosition = 0;

    public ChannelListAdapter(Activity context, List<Channel> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
    }

    public void sort(final int type) {
        switch (type) {
        case Constants.CHANNEL_SORT_DEFAULT:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    return x.compareTo(y);
                }
            });
            break;
        case Constants.CHANNEL_SORT_BY_NAME:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    return x.name.toLowerCase(Locale.US).compareTo(y.name.toLowerCase(Locale.US));
                }
            });
            break;
        case Constants.CHANNEL_SORT_BY_NUMBER:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    if (x.number > y.number) {
                        return 1;
                    } else if (x.number < y.number) {
                        return -1;
                    } else {
                        return 0;
                    }
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
        public TextView icon_text;
        public TextView title;
        public TextView channel;
        public TextView time;
        public TextView duration;
        public ProgressBar progress;
        public ImageView state;
        public TextView genre;
        public ImageView dual_pane_list_item_selection;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
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
            holder.dual_pane_list_item_selection = (ImageView) view.findViewById(R.id.dual_pane_list_item_selection);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (holder.dual_pane_list_item_selection != null) {
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
        final Channel c = getItem(position);
        if (c != null) {

            // Set the initial values
            if (holder.progress != null) {
                holder.progress.setProgress(0);
            }
            if (holder.channel != null) {
                holder.channel.setText(c.name);
            }

            Utils.setChannelIcon(holder.icon, holder.icon_text, c);
            // Only show the channel text in the program guide when no icons shall be shown
            if (holder.icon_text != null) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                final boolean showIcons = prefs.getBoolean("showIconPref", true);
                if (!showIcons && layout == R.layout.program_guide_channel_item) {
                    holder.icon_text.setText(c.name);
                    holder.icon_text.setVisibility(ImageView.VISIBLE);
                    
                    // Add the listener to the icon text so that a 
                    // click calls the program list of this channel
                    holder.icon_text.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (c.epg.isEmpty()) {
                                return;
                            }
                            if (context instanceof FragmentStatusInterface) {
                                ((FragmentStatusInterface) context).onListItemSelected(position, c, TAG);
                            }
                        }
                    });
                }
            }

            if (holder.icon != null) {
                // Add the listener to the icon so that a 
                // click calls the program list of this channel
                holder.icon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (c.epg.isEmpty()) {
                            return;
                        }
                        if (context instanceof FragmentStatusInterface) {
                            ((FragmentStatusInterface) context).onListItemSelected(position, c, TAG);
                        }
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
            
            // Get the program that is currently running
            // and set all the available values
            Program p = null;
            if (it.hasNext()) {
                p = it.next();
            }

            // Check if the channel is actually transmitting
            // data and contains program data which can be shown.
            if (!c.isTransmitting && p != null) {
                if (holder.title != null) {
                    holder.title.setText(R.string.no_transmission);
                }
            } else if (p != null) {
                if (holder.title != null) {
                    holder.title.setText(p.title);
                }
                Utils.setTime(holder.time, p.start, p.stop);
                Utils.setDuration(holder.duration, p.start, p.stop);
                Utils.setProgress(holder.progress, p.start, p.stop);
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
                if (holder.genre != null) {
                    holder.genre.setVisibility(View.GONE);
                }
            }
            Utils.setGenreColor(context, holder.genre, p, TAG);
        }
        return view;
    }

    public void update(Channel c) {
        int length = list.size();

        // Go through the list of channels and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == c.id) {
                list.set(i, c);
                break;
            }
        }
    }

    public Channel getSelectedItem() {
        if (list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}
