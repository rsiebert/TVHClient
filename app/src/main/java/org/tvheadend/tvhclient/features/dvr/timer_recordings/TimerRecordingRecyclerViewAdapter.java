package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.features.shared.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimerRecordingRecyclerViewAdapter extends RecyclerView.Adapter<TimerRecordingRecyclerViewAdapter.RecyclerViewHolder> {

    private List<TimerRecording> timerRecordingList = new ArrayList<>();
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    TimerRecordingRecyclerViewAdapter(Context context, int htspVersion) {
        this.context = context;
        this.htspVersion = htspVersion;
        this.timerRecordingList = timerRecordingList;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.timer_recording_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        TimerRecording recording = timerRecordingList.get(position);
        holder.itemView.setTag(recording);

        boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("channel_icons_enabled", true);

        if (holder.dualPaneListItemSelection != null) {
            // Set the correct indication when the dual pane mode is active
            // If the item is selected the the arrow will be shown, otherwise
            // only a vertical separation line is displayed.
            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        if (recording != null) {
            String title = !TextUtils.isEmpty(recording.getTitle()) ? recording.getTitle() : recording.getName();
            holder.titleTextView.setText(title);

            if (recording.getChannelIcon() != null) {
                UIUtils.loadIcon(context, showChannelIcons,
                        recording.getChannelIcon(), recording.getChannelName(),
                        holder.iconImageView, holder.channelTextView);
            } else {
                holder.channelTextView.setText(R.string.all_channels);
            }

            String daysOfWeek = UIUtils.getDaysOfWeekText(context, recording.getDaysOfWeek());
            holder.daysOfWeekTextView.setText(daysOfWeek);

            holder.startTimeTextView.setText(UIUtils.getTimeText(context, recording.getStart()));
            holder.stopTimeTextView.setText(UIUtils.getTimeText(context, recording.getStop()));

            String duration = context.getString(R.string.minutes, recording.getDuration());
            holder.durationTextView.setText(duration);

            String isEnabled = context.getString((recording.getEnabled() > 0) ? R.string.recording_enabled : R.string.recording_disabled);
            holder.isEnabledTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
            holder.isEnabledTextView.setText(isEnabled);
        }
    }

    void addItems(List<TimerRecording> recordingList) {
        this.timerRecordingList = recordingList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return timerRecordingList.size();
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public TimerRecording getItem(int position) {
        return timerRecordingList.get(position);
    }

    public List<TimerRecording> getItems() {
        return timerRecordingList;
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.days_of_week)
        TextView daysOfWeekTextView;
        @BindView(R.id.start)
        TextView startTimeTextView;
        @BindView(R.id.stop)
        TextView stopTimeTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @BindView(R.id.enabled)
        TextView isEnabledTextView;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection)
        ImageView dualPaneListItemSelection;

        RecyclerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
