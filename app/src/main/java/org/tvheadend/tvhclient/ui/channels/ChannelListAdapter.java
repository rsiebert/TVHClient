package org.tvheadend.tvhclient.ui.channels;

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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

// TODO lazy loading of images
// TODO move channel logo a bit down in the layout

public class ChannelListAdapter extends ArrayAdapter<Channel> implements OnClickListener {

    private final static String TAG = ChannelListAdapter.class.getSimpleName();

    private final Activity context;
    private final List<Channel> list;
    private int selectedPosition = 0;
    private final SharedPreferences sharedPreferences;
    private long showProgramsFromTime;

    public ChannelListAdapter(Activity context, List<Channel> list) {
        super(context, R.layout.channel_list_adapter, list);
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

    static class ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.icon_text)
        TextView iconTextView;
        @BindView(R.id.icon_large)
        ImageView iconLargeImageView;
        @BindView(R.id.icon_text_large)
        TextView iconLargeTextView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.subtitle)
        TextView subtitleTextView;
        @BindView(R.id.next_title)
        TextView nextTitleTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.time)
        TextView timeTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @BindView(R.id.progressbar)
        ProgressBar progressbar;
        @BindView(R.id.state)
        ImageView stateImageView;
        @BindView(R.id.genre)
        TextView genreTextView;
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
            view = context.getLayoutInflater().inflate(R.layout.channel_list_adapter, parent, false);
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

            boolean showChannelName = sharedPreferences.getBoolean("showChannelNamePref", true);
            boolean showProgressbar = sharedPreferences.getBoolean("showProgramProgressbarPref", true);
            boolean showSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);
            boolean showNextProgramTitle = sharedPreferences.getBoolean("showNextProgramPref", true);
            boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
            boolean showLargeChannelIcons = sharedPreferences.getBoolean("showBigIconPref", false);
            boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsChannelsPref", false);

            // Set the initial values
            holder.progressbar.setProgress(0);
            holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            holder.channelTextView.setText(c.channelName);
            holder.channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

            // Show the regular or large channel icons. Otherwise show the channel name only
            // Assign the channel icon image or a null image
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, c.channelIcon);
            holder.iconImageView.setImageBitmap(iconBitmap);
            holder.iconLargeImageView.setImageBitmap(iconBitmap);
            holder.iconTextView.setText(c.channelName);
            holder.iconLargeTextView.setText(c.channelName);

            // Show or hide the regular or large channel icon or name text views
            holder.iconImageView.setVisibility(showChannelIcons && !showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.iconLargeImageView.setVisibility(!showChannelIcons && !showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.iconTextView.setVisibility(showChannelIcons && showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.iconLargeTextView.setVisibility(!showChannelIcons && showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);

            // If activated in the settings allow playing
            // the program by selecting the channel icon
            holder.iconImageView.setOnClickListener(this);
            holder.iconLargeImageView.setOnClickListener(this);
            holder.iconTextView.setOnClickListener(this);
            holder.iconLargeTextView.setOnClickListener(this);

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
                holder.stateImageView.setImageResource(R.drawable.ic_rec_small);
                holder.stateImageView.setVisibility(View.VISIBLE);
            } else {
                holder.stateImageView.setImageDrawable(null);
                holder.stateImageView.setVisibility(View.GONE);
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
                holder.titleTextView.setText(p.title);
                holder.subtitleTextView.setText(p.subtitle);
                holder.subtitleTextView.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

                String time = UIUtils.getTime(context, p.start) + " - " + UIUtils.getTime(context, p.stop);
                holder.timeTextView.setText(time);

                String durationTime = context.getString(R.string.minutes, (int) ((p.stop - p.start) / 1000 / 60));
                holder.durationTextView.setText(durationTime);

                holder.progressbar.setProgress(getProgressPercentage(p.start, p.stop));
                holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

                if (np != null) {
                    holder.nextTitleTextView.setVisibility(showNextProgramTitle ? View.VISIBLE : View.GONE);
                    holder.nextTitleTextView.setText(context.getString(R.string.next_program, np.title));
                }

                if (showGenreColors) {
                    int color = UIUtils.getGenreColor(context, p.contentType, 0);
                    holder.genreTextView.setBackgroundColor(color);
                    holder.genreTextView.setVisibility(View.VISIBLE);
                } else {
                    holder.genreTextView.setVisibility(View.GONE);
                }

            } else {
                // The channel does not provide program data. Hide the progress
                // bar,the time and duration texts.
                holder.titleTextView.setText(R.string.no_data);
                holder.subtitleTextView.setVisibility(View.GONE);
                holder.progressbar.setVisibility(View.GONE);
                holder.timeTextView.setVisibility(View.GONE);
                holder.durationTextView.setVisibility(View.GONE);
                holder.genreTextView.setVisibility(View.GONE);
                holder.nextTitleTextView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    @Override
    public void onClick(View view) {
        if (sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true)) {
            Channel channel = getSelectedItem();
            new MenuUtils(context).handleMenuPlaySelection(channel.channelId, -1);
        }
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

    private int getProgressPercentage(long start, long stop) {
        // Get the start and end times to calculate the progress.
        double durationTime = (stop - start);
        double elapsedTime = new Date().getTime() - start;

        // Show the progress as a percentage
        double percentage = 0;
        if (durationTime > 0) {
            percentage = elapsedTime / durationTime;
        }
        return (int) Math.floor(percentage * 100);
    }
}
