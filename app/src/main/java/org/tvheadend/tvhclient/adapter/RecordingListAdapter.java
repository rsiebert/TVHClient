package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecordingListAdapter extends ArrayAdapter<Recording> {

    private final Activity context;
    private final List<Recording> list;
    private final SharedPreferences sharedPreferences;
    private final int htspVersion;
    private int selectedPosition = 0;

    public RecordingListAdapter(Activity context, List<Recording> list) {
        super(context, R.layout.recording_list_widget, list);
        this.context = context;
        this.list = list;
        this.htspVersion = DataStorage.getInstance().getProtocolVersion();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sort(final int type) {
        switch (type) {
            case Constants.RECORDING_SORT_ASCENDING:
                sort(new Comparator<Recording>() {
                    public int compare(Recording x, Recording y) {
                        if (y.start == x.start) {
                            return 1;
                        } else if (x.start < y.start) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                break;
            case Constants.RECORDING_SORT_DESCENDING:
                sort(new Comparator<Recording>() {
                    public int compare(Recording x, Recording y) {
                        if (y.start == x.start) {
                            return 1;
                        } else if (x.start > y.start) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                break;
        }
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public List<Recording> getAllItems() {
        return list;
    }

    static class ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.icon_text)
        TextView iconTextView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.subtitle)
        TextView subtitleTextView;
        @BindView(R.id.summary)
        TextView summaryTextView;
        @BindView(R.id.is_series_recording)
        TextView isSeriesRecordingTextView;
        @BindView(R.id.is_timer_recording)
        TextView isTimerRecordingTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.time)
        TextView timeTextView;
        @BindView(R.id.date)
        TextView dateTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @Nullable
        @BindView(R.id.state)
        ImageView stateImageView;
        @BindView(R.id.description)
        TextView descriptionTextView;
        @BindView(R.id.failed_reason)
        TextView failedReasonTextView;
        @BindView(R.id.enabled)
        TextView isEnabledTextView;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection)
        ImageView dual_pane_list_item_selection;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }


    @NonNull
    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = context.getLayoutInflater().inflate(R.layout.recording_list_widget, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        boolean playOnChannelIcon = sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true);
        boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);

        holder.iconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playOnChannelIcon) {
                    Recording recording = getSelectedItem();
                    if (recording.isCompleted() || recording.isRecording()) {
                        new MenuUtils(context).handleMenuPlaySelection(-1, recording.id);
                    }
                }
            }
        });
        if (holder.dual_pane_list_item_selection != null) {
            // Set the correct indication when the dual pane mode is active
            // If the item is selected the the arrow will be shown, otherwise
            // only a vertical separation line is displayed.
            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dual_pane_list_item_selection.setBackgroundResource(icon);
            }
        }

        // Get the program and assign all the values
        final Recording rec = getItem(position);
        if (rec != null) {
            Channel channel = DataStorage.getInstance().getChannelFromArray(rec.channel);
            holder.titleTextView.setText(rec.title);
            if (channel != null) {
                holder.channelTextView.setText(channel.channelName);
            }

            if (!TextUtils.isEmpty(rec.subtitle)) {
                holder.subtitleTextView.setVisibility(View.VISIBLE);
                holder.subtitleTextView.setText(rec.subtitle);
            } else {
                holder.subtitleTextView.setVisibility(View.GONE);
            }

            // Show the channel icon if available and set in the preferences.
            // If not chosen, hide the imageView and show the channel name.
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.channelIcon);
            // Show the icon or a blank one if it does not exist
            holder.iconImageView.setImageBitmap(iconBitmap);
            holder.iconTextView.setText(channel.channelName);
            // Show the channels icon if set in the preferences.
            // If not then hide the icon and show the channel name as a placeholder
            holder.iconImageView.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.iconTextView.setVisibility(showChannelIcons ? ImageView.GONE : ImageView.VISIBLE);

            Utils.setDate(holder.dateTextView, rec.start);
            Utils.setTime(holder.timeTextView, rec.start, rec.stop);
            Utils.setDuration(holder.durationTextView, rec.start, rec.stop);
            Utils.setDescription(null, holder.summaryTextView, rec.summary);
            Utils.setDescription(null, holder.descriptionTextView, rec.description);
            Utils.setFailedReason(holder.failedReasonTextView, rec);

            // Show only the recording icon
            if (rec.isRecording()) {
                holder.stateImageView.setImageResource(R.drawable.ic_rec_small);
                holder.stateImageView.setVisibility(ImageView.VISIBLE);
            } else {
                holder.stateImageView.setVisibility(ImageView.GONE);
            }

            // Show the information if the recording belongs to a series recording
            if (rec.autorecId != null) {
                holder.isSeriesRecordingTextView.setVisibility(ImageView.VISIBLE);
            } else {
                holder.isSeriesRecordingTextView.setVisibility(ImageView.GONE);
            }

            // Show the information if the recording belongs to a series recording
            if (rec.timerecId != null) {
                holder.isTimerRecordingTextView.setVisibility(ImageView.VISIBLE);
            } else {
                holder.isTimerRecordingTextView.setVisibility(ImageView.GONE);
            }

            holder.isEnabledTextView.setVisibility((htspVersion >= 19 && rec.enabled > 0) ? View.VISIBLE : View.GONE);
            holder.isEnabledTextView.setText(rec.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);
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

    public Recording getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}