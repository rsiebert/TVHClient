package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
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
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimerRecordingRecyclerViewAdapter extends RecyclerView.Adapter<TimerRecordingRecyclerViewAdapter.RecyclerViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private List<TimerRecording> recordingList = new ArrayList<>();
    private List<TimerRecording> recordingListFiltered = new ArrayList<>();
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    TimerRecordingRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback, int htspVersion) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.htspVersion = htspVersion;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.timer_recording_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        TimerRecording recording = recordingListFiltered.get(position);
        holder.itemView.setTag(recording);

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

            TextViewCompat.setAutoSizeTextTypeWithDefaults(holder.iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

            if (!TextUtils.isEmpty(recording.getChannelName())) {
                holder.iconTextView.setText(recording.getChannelName());
                holder.channelTextView.setText(recording.getChannelName());
            } else {
                holder.iconTextView.setText(R.string.all_channels);
                holder.channelTextView.setText(R.string.all_channels);
            }

            Picasso.get()
                    .load(UIUtils.getIconUrl(context, recording.getChannelIcon()))
                    .into(holder.iconImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.iconTextView.setVisibility(View.INVISIBLE);
                            holder.iconImageView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });

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

    void addItems(List<TimerRecording> list) {
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

    public TimerRecording getItem(int position) {
        if (recordingListFiltered.size() > position && position >= 0) {
            return recordingListFiltered.get(position);
        } else {
            return null;
        }
    }

    public List<TimerRecording> getItems() {
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
                    List<TimerRecording> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (TimerRecording recording : new CopyOnWriteArrayList<>(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (recording.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording);
                        } else if (recording.getName() != null
                                && recording.getName().toLowerCase().contains(charString.toLowerCase())) {
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
                recordingListFiltered = (ArrayList<TimerRecording>) filterResults.values;
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
