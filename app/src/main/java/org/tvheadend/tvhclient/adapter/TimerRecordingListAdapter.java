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
import org.tvheadend.tvhclient.model.TimerRecording;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimerRecordingListAdapter extends ArrayAdapter<TimerRecording> {

    private final Activity context;
    private final List<TimerRecording> list;
    private int selectedPosition = 0;

    public TimerRecordingListAdapter(Activity context, List<TimerRecording> list) {
        super(context, 0);
        this.context = context;
        this.list = list;
    }

    public void sort(final int type) {
        switch (type) {
            case Constants.RECORDING_SORT_ASCENDING:
                sort(new Comparator<TimerRecording>() {
                    public int compare(TimerRecording x, TimerRecording y) {
                        if (x != null && y != null && x.title != null && y.title != null) {
                            return (y.title.compareTo(x.title));
                        }
                        return 0;
                    }
                });
                break;
            case Constants.RECORDING_SORT_DESCENDING:
                sort(new Comparator<TimerRecording>() {
                    public int compare(TimerRecording x, TimerRecording y) {
                        if (x != null && y != null && x.title != null && y.title != null) {
                            return (x.title.compareTo(y.title));
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
        @BindView(R.id.icon) ImageView icon;
        @BindView(R.id.title) TextView title;
        @BindView(R.id.channel) TextView channel;
        @BindView(R.id.daysOfWeek) TextView daysOfWeek;
        @BindView(R.id.time) TextView time;
        @BindView(R.id.duration) TextView duration;
        @BindView(R.id.enabled) TextView isEnabled;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection) ImageView dual_pane_list_item_selection;

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
            view = context.getLayoutInflater().inflate(R.layout.timer_recording_list_widget, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
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
        TimerRecording trec = getItem(position);
        if (trec != null) {
            Channel channel = DataStorage.getInstance().getChannelFromArray(trec.channel);
            if (!TextUtils.isEmpty(trec.title)) {
                holder.title.setText(trec.title);
            } else {
                holder.title.setText(trec.name);
            }

            if (channel != null) {
                holder.channel.setText(channel.channelName);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean showChannelIcons = prefs.getBoolean("showIconPref", true);
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.channelIcon);
                holder.icon.setImageBitmap(iconBitmap);
                holder.icon.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            } else {
                holder.channel.setText(R.string.all_channels);
            }

            Utils.setDaysOfWeek(context, null, holder.daysOfWeek, trec.daysOfWeek);

            // TODO multiple uses, consolidate
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, trec.start / 60);
            startTime.set(Calendar.MINUTE, trec.start % 60);

            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.HOUR_OF_DAY, trec.stop / 60);
            endTime.set(Calendar.MINUTE, trec.stop % 60);

            Utils.setTime(holder.time, startTime.getTimeInMillis(), endTime.getTimeInMillis());
            holder.duration.setText(context.getString(R.string.minutes, (int) (trec.stop - trec.start)));
            holder.isEnabled.setVisibility(DataStorage.getInstance().getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED ? View.VISIBLE : View.GONE);
            holder.isEnabled.setText(trec.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);
        }
        return view;
    }

    public void update(TimerRecording trec) {
        int length = list.size();
        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id.compareTo(trec.id) == 0) {
                list.set(i, trec);
                break;
            }
        }
    }

    public TimerRecording getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }

    public List<TimerRecording> getAllItems() {
        return list;
    }
}