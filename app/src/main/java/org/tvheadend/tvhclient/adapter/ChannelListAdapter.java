package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.Utils;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelListAdapter extends ArrayAdapter<Channel> {

    private final static String TAG = ChannelListAdapter.class.getSimpleName();

    private final Activity context;
    private final List<Channel> list;
    private final int layout;
    private int selectedPosition = 0;
    private final SharedPreferences prefs;
    private long showProgramsFromTime;

    public ChannelListAdapter(Activity context, List<Channel> list, int layout) {
        super(context, layout, list);
        this.context = context;
        this.layout = layout;
        this.list = list;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setTime(final long time) {
        this.showProgramsFromTime = time;
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
        public LinearLayout channel_item_layout;
        public ImageView icon;
        public TextView icon_text;
        public TextView title;
        public TextView subtitle;
        public TextView nextTitle;
        public TextView channel;
        public TextView time;
        public TextView duration;
        public ProgressBar progressbar;
        public ImageView state;
        public TextView genre;
        public ImageView dual_pane_list_item_selection;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(layout, parent, false);
            holder = new ViewHolder();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean bigIcon = prefs.getBoolean("showBigIconPref", false);
            holder.icon = (ImageView) view.findViewById(bigIcon ? R.id.icon_large : R.id.icon);

            holder.channel_item_layout = (LinearLayout) view.findViewById(R.id.channel_item_layout);
            holder.icon_text = (TextView) view.findViewById(R.id.icon_text);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
            holder.nextTitle = (TextView) view.findViewById(R.id.next_title);
            holder.channel = (TextView) view.findViewById(R.id.channel);
            holder.progressbar = (ProgressBar) view.findViewById(R.id.progressbar);
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
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            }
        }

        // Get the program and assign all the values
        final Channel c = getItem(position);
        if (c != null) {

            // Set the initial values
            if (holder.progressbar != null) {
                holder.progressbar.setProgress(0);
                holder.progressbar.setVisibility(prefs.getBoolean("showProgramProgressbarPref", true) ? View.VISIBLE : View.GONE);
            }
            if (holder.channel != null) {
                holder.channel.setText(c.name);
                holder.channel.setVisibility(prefs.getBoolean("showChannelNamePref", true) ? View.VISIBLE : View.GONE);
            }

            Utils.setChannelIcon(holder.icon, holder.icon_text, c);
            // Only show the channel text in the program guide when no icons shall be shown
            if (holder.icon_text != null) {
                final boolean showIcons = prefs.getBoolean("showIconPref", true);
                if (!showIcons && layout == R.layout.program_guide_channel_item) {
                    holder.icon_text.setText(c.name);
                    holder.icon_text.setVisibility(ImageView.VISIBLE);
                }
                // If activated in the settings allow playing 
                // the program by selecting the channel icon
                holder.icon_text.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (context instanceof FragmentStatusInterface) {
                            ((FragmentStatusInterface) context).onListItemSelected(position, c, Constants.TAG_CHANNEL_ICON);
                        }
                    }
                });
            }

            if (holder.icon != null) {
                // If activated in the settings allow playing 
                // the program by selecting the channel icon 
                holder.icon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (context instanceof FragmentStatusInterface) {
                            ((FragmentStatusInterface) context).onListItemSelected(position, c, Constants.TAG_CHANNEL_ICON);
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

            CopyOnWriteArrayList<Program> epg = new CopyOnWriteArrayList<>(c.epg);
            Program p = null;
            int availableProgramCount = epg.size();
            boolean currentProgramFound = false;
            Iterator<Program> it = epg.iterator();

            // Search through the EPG and find the first program that is currently running.
            // Also count how many programs are available without counting the ones in the past.
            while (it.hasNext()) {
                p = it.next();
                if (p.start.getTime() >= showProgramsFromTime ||
                    p.stop.getTime() >= showProgramsFromTime) {
                    currentProgramFound = true;
                    break;
                } else {
                    availableProgramCount--;
                }
            }

            if ((!currentProgramFound || availableProgramCount < Constants.PROGRAMS_VISIBLE_BEFORE_LOADING_MORE) &&
                    layout != R.layout.program_guide_channel_item) {
                Utils.loadMorePrograms(context, c);
            }

            Program np = null;
            if (it.hasNext()) {
                np = it.next();
            }

            // Check if the channel is actually transmitting
            // data and contains program data which can be shown.
            if (!c.isTransmitting && p != null) {
                if (holder.title != null) {
                    holder.title.setText(R.string.no_transmission);
                }
                if (holder.subtitle != null) {
                    holder.subtitle.setVisibility(View.GONE);
                }
                if (holder.nextTitle != null) {
                    holder.nextTitle.setVisibility(View.GONE);
                }
            } else if (p != null) {
                if (holder.title != null) {
                    holder.title.setText(p.title);
                }
                if (holder.subtitle != null) {
                    holder.subtitle.setText(p.subtitle);
                    holder.subtitle.setVisibility(prefs.getBoolean("showProgramSubtitlePref", true) ? View.VISIBLE : View.GONE);
                }
                Utils.setTime(holder.time, p.start, p.stop);
                Utils.setDuration(holder.duration, p.start, p.stop);

                if (holder.progressbar != null) {
                    Utils.setProgress(holder.progressbar, p.start, p.stop);
                    holder.progressbar.setVisibility(prefs.getBoolean("showProgramProgressbarPref", true) ? View.VISIBLE : View.GONE);
                }
                if (holder.nextTitle != null && np != null) {
                    holder.nextTitle.setVisibility(prefs.getBoolean("showNextProgramPref", true) ? View.VISIBLE : View.GONE);
                    holder.nextTitle.setText(context.getString(R.string.next_program, np.title));
                }
            }
            else {
                // The channel does not provide program data. Hide the progress
                // bar,the time and duration texts.
                if (holder.title != null) {
                    holder.title.setText(R.string.no_data);
                }
                if (holder.subtitle != null) {
                    holder.subtitle.setVisibility(View.GONE);
                }
                if (holder.progressbar != null) {
                    holder.progressbar.setVisibility(View.GONE);
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
                if (holder.nextTitle != null) {
                    holder.nextTitle.setVisibility(View.GONE);
                }
            }
            if (layout == R.layout.program_guide_channel_item) {
                Utils.setGenreColor(context, holder.channel_item_layout, p, TAG);
            } else {
                Utils.setGenreColor(context, holder.genre, p, TAG);
            }

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
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}
