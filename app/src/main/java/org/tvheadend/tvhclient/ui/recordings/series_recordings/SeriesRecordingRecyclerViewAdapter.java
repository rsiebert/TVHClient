package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeriesRecordingRecyclerViewAdapter extends RecyclerView.Adapter<SeriesRecordingRecyclerViewAdapter.RecyclerViewHolder> {

    private List<SeriesRecording> seriesRecordingList;
    private RecyclerViewClickCallback clickCallback;
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    SeriesRecordingRecyclerViewAdapter(Context context, List<SeriesRecording> seriesRecordingList, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.htspVersion = new DataRepository(context).getHtspVersion();
        this.seriesRecordingList = seriesRecordingList;
        this.clickCallback = clickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.series_recording_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        SeriesRecording recording = seriesRecordingList.get(position);
        holder.itemView.setTag(recording);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClick(view, holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return clickCallback.onLongClick(view);
            }
        });

        boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);

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
            holder.titleTextView.setText(recording.getTitle());

            if (!TextUtils.isEmpty(recording.getName())) {
                holder.nameTextView.setVisibility(View.VISIBLE);
                holder.nameTextView.setText(recording.getName());
            } else {
                holder.nameTextView.setVisibility(View.GONE);
            }
            if (recording.getChannelIcon() != null) {
                holder.channelTextView.setText(recording.getChannelName());
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, recording.getChannelIcon());
                holder.iconImageView.setImageBitmap(iconBitmap);
                holder.iconImageView.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            } else {
                holder.channelTextView.setText(R.string.all_channels);
            }

            holder.daysOfWeekTextView.setText(UIUtils.getDaysOfWeekText(context, recording.getDaysOfWeek()));

            // Convert the minute from midnight into a time
            String time = UIUtils.getTime(context, recording.getStart()) + " - " + UIUtils.getTime(context, recording.getStartWindow());
            holder.timeTextView.setText(time);
            // Show the duration
            holder.durationTextView.setText(context.getString(R.string.minutes, recording.getDuration()));

            holder.isEnabledTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
            holder.isEnabledTextView.setText(recording.getEnabled() > 0 ? R.string.recording_enabled : R.string.recording_disabled);
        }
    }

    void addItems(List<SeriesRecording> recordingList) {
        this.seriesRecordingList = recordingList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return seriesRecordingList.size();
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public SeriesRecording getItem(int position) {
        return seriesRecordingList.get(position);
    }

    public List<SeriesRecording> getItems() {
        return seriesRecordingList;
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.name)
        TextView nameTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.days_of_week)
        TextView daysOfWeekTextView;
        @BindView(R.id.time)
        TextView timeTextView;
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
