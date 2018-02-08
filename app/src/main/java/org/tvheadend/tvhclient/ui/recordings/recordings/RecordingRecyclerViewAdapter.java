package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.app.Activity;
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
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

// TODO improve layout

public class RecordingRecyclerViewAdapter extends RecyclerView.Adapter<RecordingRecyclerViewAdapter.RecyclerViewHolder> {

    private List<Recording> recordingList;
    private RecyclerViewClickCallback clickCallback;
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Activity activity;
    private int selectedPosition = 0;

    RecordingRecyclerViewAdapter(Activity activity, List<Recording> recordingList, int htspVersion, RecyclerViewClickCallback clickCallback) {
        this.activity = activity;
        this.htspVersion = htspVersion;
        this.recordingList = recordingList;
        this.clickCallback = clickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recording_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Recording recording = recordingList.get(position);
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

        boolean playOnChannelIcon = sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true);
        boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);

        holder.iconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playOnChannelIcon && recording != null) {
                    if (recording.isCompleted() || recording.isRecording()) {
                        new MenuUtils(activity).handleMenuPlaySelection(-1, recording.getId());
                    }
                }
            }
        });

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
            if (recording.getChannelIcon() != null) {
                holder.channelTextView.setText(recording.getChannelName());
            }

            if (!TextUtils.isEmpty(recording.getSubtitle())) {
                holder.subtitleTextView.setVisibility(View.VISIBLE);
                holder.subtitleTextView.setText(recording.getSubtitle());
            } else {
                holder.subtitleTextView.setVisibility(View.GONE);
            }

            // Show the channel icon if available and set in the preferences.
            // If not chosen, hide the imageView and show the channel name.
            Bitmap iconBitmap = null;
            if (recording.getChannelIcon() != null) {
                iconBitmap = MiscUtils.getCachedIcon(activity, recording.getChannelIcon());
                // Show the icon or a blank one if it does not exist
                holder.iconImageView.setImageBitmap(iconBitmap);
                holder.iconTextView.setText(recording.getChannelName());
            }

            if (showChannelIcons) {
                holder.iconImageView.setVisibility(iconBitmap != null ? ImageView.VISIBLE : ImageView.INVISIBLE);
                holder.iconTextView.setVisibility(iconBitmap == null ? ImageView.VISIBLE : ImageView.INVISIBLE);
            } else {
                holder.iconImageView.setVisibility(View.GONE);
                holder.iconTextView.setVisibility(View.GONE);
            }

            holder.dateTextView.setText(UIUtils.getDate(activity, recording.getStart()));

            holder.startTimeTextView.setText(UIUtils.getTimeText(activity, recording.getStart()));
            holder.stopTimeTextView.setText(UIUtils.getTimeText(activity, recording.getStop()));

            String durationTime = activity.getString(R.string.minutes, recording.getDuration());
            holder.durationTextView.setText(durationTime);

            holder.summaryTextView.setVisibility(!TextUtils.isEmpty(recording.getSummary()) ? View.VISIBLE : View.GONE);
            holder.summaryTextView.setText(recording.getSummary());

            holder.descriptionTextView.setVisibility(!TextUtils.isEmpty(recording.getDescription()) ? View.VISIBLE : View.GONE);
            holder.descriptionTextView.setText(recording.getDescription());

            if (recording.isRemoved()) {
                holder.failedReasonTextView.setVisibility(View.GONE);
            } else if (recording.isAborted()) {
                holder.failedReasonTextView.setText(activity.getResources().getString(R.string.recording_canceled));
            } else if (recording.isMissed()) {
                holder.failedReasonTextView.setText(activity.getResources().getString(R.string.recording_time_missed));
            } else if (recording.isFailed()) {
                holder.failedReasonTextView.setText(activity.getResources().getString(R.string.recording_file_invalid));
            } else {
                holder.failedReasonTextView.setVisibility(View.GONE);
            }

            // Show only the recording icon
            if (holder.stateImageView != null) {
                if (recording.isRecording()) {
                    holder.stateImageView.setImageResource(R.drawable.ic_rec_small);
                    holder.stateImageView.setVisibility(ImageView.VISIBLE);
                } else {
                    holder.stateImageView.setVisibility(ImageView.GONE);
                }
            }

            // Show the information if the recording belongs to a series recording
            if (!TextUtils.isEmpty(recording.getAutorecId())) {
                holder.isSeriesRecordingTextView.setVisibility(ImageView.VISIBLE);
            } else {
                holder.isSeriesRecordingTextView.setVisibility(ImageView.GONE);
            }

            // Show the information if the recording belongs to a series recording
            if (!TextUtils.isEmpty(recording.getTimerecId())) {
                holder.isTimerRecordingTextView.setVisibility(ImageView.VISIBLE);
            } else {
                holder.isTimerRecordingTextView.setVisibility(ImageView.GONE);
            }

            holder.isEnabledTextView.setVisibility((htspVersion >= 19 && recording.getEnabled() > 0) ? View.VISIBLE : View.GONE);
            holder.isEnabledTextView.setText(recording.getEnabled() > 0 ? R.string.recording_enabled : R.string.recording_disabled);
        }
    }

    void addItems(List<Recording> recordingList) {
        this.recordingList = recordingList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public Recording getItem(int position) {
        return recordingList.get(position);
    }

    public List<Recording> getItems() {
        return recordingList;
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
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
        @BindView(R.id.start)
        TextView startTimeTextView;
        @BindView(R.id.stop)
        TextView stopTimeTextView;
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
        ImageView dualPaneListItemSelection;

        RecyclerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
