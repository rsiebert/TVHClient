package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                        if (x != null && y != null) {
                            return x.channelName.compareTo(y.channelName);
                        }
                        return 0;
                    }
                });
                break;
            case Constants.CHANNEL_SORT_BY_NAME:
                sort(new Comparator<Channel>() {
                    public int compare(Channel x, Channel y) {
                        if (x != null && y != null) {
                            return x.channelName.toLowerCase(Locale.US).compareTo(y.channelName.toLowerCase(Locale.US));
                        }
                        return 0;
                    }
                });
                break;
            case Constants.CHANNEL_SORT_BY_NUMBER:
                sort(new Comparator<Channel>() {
                    public int compare(Channel x, Channel y) {
                        if (x != null && y != null) {

                            if (x.channelNumber > y.channelNumber) {
                                return 1;
                            } else if (x.channelNumber < y.channelNumber) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                        return 0;
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
            holder.icon = view.findViewById(bigIcon ? R.id.icon_large : R.id.icon);

            holder.channel_item_layout = view.findViewById(R.id.channel_item_layout);
            holder.icon_text = view.findViewById(bigIcon ? R.id.icon_text_large : R.id.icon_text);
            holder.title = view.findViewById(R.id.title);
            holder.subtitle = view.findViewById(R.id.subtitle);
            holder.nextTitle = view.findViewById(R.id.next_title);
            holder.channel = view.findViewById(R.id.channel);
            holder.progressbar = view.findViewById(R.id.progressbar);
            holder.time = view.findViewById(R.id.time);
            holder.duration = view.findViewById(R.id.duration);
            holder.state = view.findViewById(R.id.state);
            holder.genre = view.findViewById(R.id.genre);
            holder.dual_pane_list_item_selection = view.findViewById(R.id.dual_pane_list_item_selection);
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
                holder.channel.setText(c.channelName);
                holder.channel.setVisibility(prefs.getBoolean("showChannelNamePref", true) ? View.VISIBLE : View.GONE);
            }
            // Show the channel icon if available and set in the preferences.
            // If not chosen, hide the imageView and show the channel name.
            if (holder.icon != null && holder.icon_text != null) {
                final boolean showChannelIcons = prefs.getBoolean("showIconPref", true);
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, c.channelIcon);
                // Show the icon or a blank one if it does not exist
                holder.icon.setImageBitmap(iconBitmap);
                holder.icon_text.setText(c.channelName);
                // Show the channels icon if set in the preferences.
                // If not then hide the icon and show the channel name as a placeholder
                holder.icon.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
                holder.icon_text.setVisibility(showChannelIcons ? ImageView.GONE : ImageView.VISIBLE);

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

            // Add a small recording icon above the channel icon, if we are
            // recording the current program.
            if (holder.state != null) {
                boolean isRecording = false;
                Map<Integer, Recording> recMap = DataStorage.getInstance().getRecordingsFromArray();
                for (Recording rec : recMap.values()) {
                    if (rec.eventId == c.eventId) {
                        isRecording = rec.isRecording();
                        break;
                    }
                }
                if (isRecording) {
                    holder.state.setImageResource(R.drawable.ic_rec_small);
                    holder.state.setVisibility(View.VISIBLE);
                } else {
                    holder.state.setImageDrawable(null);
                    holder.state.setVisibility(View.GONE);
                }
            }

            Program p = null;
            Program np = null;

            for (Program program : DataStorage.getInstance().getProgramsFromArray().values()) {
                if (program.channelId == c.channelId) {
                    if ((program.start * 1000) <= showProgramsFromTime && (program.stop * 1000) > showProgramsFromTime) {
                        p = program;
                        break;
                    }
                }
            }

            if (p != null) {
                np = DataStorage.getInstance().getProgramFromArray(p.nextEventId);
            }

            // Check if the channel is actually transmitting
            // data and contains program data which can be shown.
            if (c.eventId == 0) {
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
                Utils.setTime2(holder.time, p.start, p.stop);
                Utils.setDuration2(holder.duration, p.start, p.stop);

                if (holder.progressbar != null) {
                    Utils.setProgress2(holder.progressbar, p.start, p.stop);
                    holder.progressbar.setVisibility(prefs.getBoolean("showProgramProgressbarPref", true) ? View.VISIBLE : View.GONE);
                }
                if (holder.nextTitle != null && np != null) {
                    holder.nextTitle.setVisibility(prefs.getBoolean("showNextProgramPref", true) ? View.VISIBLE : View.GONE);
                    holder.nextTitle.setText(context.getString(R.string.next_program, np.title));
                }
            } else {
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
            if (p != null) {
                if (layout == R.layout.program_guide_channel_item) {
                    MiscUtils.setGenreColor(context, holder.channel_item_layout, p.contentType, TAG);
                } else {
                    MiscUtils.setGenreColor(context, holder.genre, p.contentType, TAG);
                }
            }
        }
        return view;
    }

    public void update(Channel c) {
        int length = list.size();

        // Go through the list of channels and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).channelId == c.channelId) {
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
