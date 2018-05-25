package org.tvheadend.tvhclient.features.dvr.recordings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecordingRecyclerViewAdapter extends RecyclerView.Adapter<RecordingRecyclerViewAdapter.RecyclerViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private List<Recording> recordingList = new ArrayList<>();
    private List<Recording> recordingListFiltered = new ArrayList<>();
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Activity activity;
    private int selectedPosition = 0;

    RecordingRecyclerViewAdapter(Activity activity, RecyclerViewClickCallback clickCallback, int htspVersion) {
        this.activity = activity;
        this.clickCallback = clickCallback;
        this.htspVersion = htspVersion;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recording_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Recording recording = recordingListFiltered.get(position);
        holder.itemView.setTag(recording);

        boolean playOnChannelIcon = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);
        boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClick(view, holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                clickCallback.onLongClick(view, holder.getAdapterPosition());
                return true;
            }
        });
        holder.iconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playOnChannelIcon && recording != null
                        && (recording.isCompleted() || recording.isRecording())) {
                    clickCallback.onClick(view, holder.getAdapterPosition());
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

        holder.titleTextView.setText(recording.getTitle());

        holder.subtitleTextView.setVisibility(!TextUtils.isEmpty(recording.getSubtitle()) ? View.VISIBLE : View.GONE);
        holder.subtitleTextView.setText(recording.getSubtitle());

        holder.channelTextView.setText(recording.getChannelName());

        // Show the channel icon if available and set in the preferences.
        // If not chosen, hide the imageView and show the channel name.
        Picasso.get()
                .load(UIUtils.getIconUrl(activity, recording.getChannelIcon()))
                .into(holder.iconImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        holder.iconTextView.setVisibility(View.INVISIBLE);
                        holder.iconImageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        holder.iconTextView.setVisibility(View.VISIBLE);
                        holder.iconImageView.setVisibility(View.INVISIBLE);
                    }
                });

        holder.dateTextView.setText(UIUtils.getDate(activity, recording.getStart()));

        holder.startTimeTextView.setText(UIUtils.getTimeText(activity, recording.getStart()));
        holder.stopTimeTextView.setText(UIUtils.getTimeText(activity, recording.getStop()));

        String durationTime = activity.getString(R.string.minutes, recording.getDuration());
        holder.durationTextView.setText(durationTime);

        holder.summaryTextView.setVisibility(!TextUtils.isEmpty(recording.getSummary()) ? View.VISIBLE : View.GONE);
        holder.summaryTextView.setText(recording.getSummary());

        holder.descriptionTextView.setVisibility(!TextUtils.isEmpty(recording.getDescription()) ? View.VISIBLE : View.GONE);
        holder.descriptionTextView.setText(recording.getDescription());

        String failedReasonText = UIUtils.getRecordingFailedReasonText(activity, recording);
        holder.failedReasonTextView.setVisibility(!TextUtils.isEmpty(failedReasonText) ? View.VISIBLE : View.GONE);
        holder.failedReasonTextView.setText(failedReasonText);

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

    void addItems(List<Recording> list) {
        recordingList.clear();
        recordingListFiltered.clear();

        if (list != null) {
            recordingList = list;
            recordingListFiltered = list;
        }

        if (list == null || selectedPosition > list.size()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordingListFiltered != null ? recordingListFiltered.size() : 0;
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public Recording getItem(int position) {
        if (recordingListFiltered.size() > position && position >= 0) {
            return recordingListFiltered.get(position);
        } else {
            return null;
        }
    }

    public List<Recording> getItems() {
        return recordingListFiltered;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    recordingListFiltered = recordingList;
                } else {
                    List<Recording> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (Recording recording : new CopyOnWriteArrayList<>(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (recording.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording);
                        } else if (recording.getSubtitle() != null
                                && recording.getSubtitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording);
                        }
                    }
                    recordingListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = recordingListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                recordingListFiltered = (ArrayList<Recording>) filterResults.values;
                notifyDataSetChanged();
            }
        };
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
