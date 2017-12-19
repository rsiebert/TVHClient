package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelListAdapter extends ArrayAdapter<Channel> implements OnClickListener {

    private final static String TAG = ChannelListAdapter.class.getSimpleName();

    private final Activity context;
    private final List<Channel> list;
    private int selectedPosition = 0;
    private final SharedPreferences sharedPreferences;
    private long showProgramsFromTime;

    public ChannelListAdapter(Activity context, List<Channel> list) {
        super(context, R.layout.channel_list_widget, list);
        this.context = context;
        this.list = list;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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

    @Override
    public void onClick(View view) {
        if (sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true)) {
            Channel channel = getSelectedItem();
            new MenuUtils(context).handleMenuPlaySelection(channel.channelId, -1);
        }
    }

    static class ViewHolder {
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.icon_text)
        TextView icon_text;
        @BindView(R.id.icon_large)
        ImageView icon_large;
        @BindView(R.id.icon_text_large)
        TextView icon_text_large;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.subtitle)
        TextView subtitle;
        @BindView(R.id.next_title)
        TextView nextTitle;
        @BindView(R.id.channel)
        TextView channel;
        @BindView(R.id.time)
        TextView time;
        @BindView(R.id.duration)
        TextView duration;
        @BindView(R.id.progressbar)
        ProgressBar progressbar;
        @BindView(R.id.state)
        ImageView state;
        @BindView(R.id.genre)
        TextView genre;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection)
        ImageView dual_pane_list_item_selection;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = context.getLayoutInflater().inflate(R.layout.channel_list_widget, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (holder.dual_pane_list_item_selection != null) {
            boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
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
            holder.progressbar.setProgress(0);
            holder.progressbar.setVisibility(sharedPreferences.getBoolean("showProgramProgressbarPref", true) ? View.VISIBLE : View.GONE);

            holder.channel.setText(c.channelName);
            holder.channel.setVisibility(sharedPreferences.getBoolean("showChannelNamePref", true) ? View.VISIBLE : View.GONE);

            // Show the regular or large channel icons. Otherwise show the channel name only
            boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
            boolean bigIcon = sharedPreferences.getBoolean("showBigIconPref", false);

            // Assign the channel icon image or a null image
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, c.channelIcon);
            holder.icon.setImageBitmap(iconBitmap);
            holder.icon_large.setImageBitmap(iconBitmap);
            holder.icon_text.setText(c.channelName);
            holder.icon_text_large.setText(c.channelName);

            // Show or hide the regular or large channel icon or name text views
            holder.icon.setVisibility(showChannelIcons && !bigIcon ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_text.setVisibility(!showChannelIcons && !bigIcon ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_large.setVisibility(showChannelIcons && bigIcon ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_text_large.setVisibility(!showChannelIcons && bigIcon ? ImageView.VISIBLE : ImageView.GONE);

            // If activated in the settings allow playing
            // the program by selecting the channel icon
            holder.icon.setOnClickListener(this);
            holder.icon_large.setOnClickListener(this);
            holder.icon_text.setOnClickListener(this);
            holder.icon_text_large.setOnClickListener(this);

            // Add a small recording icon above the channel icon, if we are
            // recording the current program.
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

            Program p = null;
            Program np = null;

            for (Program program : DataStorage.getInstance().getProgramsFromArray().values()) {
                if (program.channelId == c.channelId) {
                    if (program.start <= showProgramsFromTime && program.stop > showProgramsFromTime) {
                        p = program;
                        np = DataStorage.getInstance().getProgramFromArray(program.nextEventId);
                        break;
                    }
                }
            }

            // Check if the channel is actually transmitting
            // data and contains program data which can be shown.
            if (p != null) {
                holder.title.setText(p.title);
                holder.subtitle.setText(p.subtitle);
                holder.subtitle.setVisibility(sharedPreferences.getBoolean("showProgramSubtitlePref", true) ? View.VISIBLE : View.GONE);

                Utils.setTime(holder.time, p.start, p.stop);
                Utils.setDuration(holder.duration, p.start, p.stop);

                Utils.setProgress(holder.progressbar, p.start, p.stop);
                holder.progressbar.setVisibility(sharedPreferences.getBoolean("showProgramProgressbarPref", true) ? View.VISIBLE : View.GONE);

                if (np != null) {
                    holder.nextTitle.setVisibility(sharedPreferences.getBoolean("showNextProgramPref", true) ? View.VISIBLE : View.GONE);
                    holder.nextTitle.setText(context.getString(R.string.next_program, np.title));
                }
                MiscUtils.setGenreColor(context, holder.genre, p.contentType, TAG);

            } else {
                // The channel does not provide program data. Hide the progress
                // bar,the time and duration texts.
                holder.title.setText(R.string.no_data);
                holder.subtitle.setVisibility(View.GONE);
                holder.progressbar.setVisibility(View.GONE);
                holder.time.setVisibility(View.GONE);
                holder.duration.setVisibility(View.GONE);
                holder.genre.setVisibility(View.GONE);
                holder.nextTitle.setVisibility(View.GONE);
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
