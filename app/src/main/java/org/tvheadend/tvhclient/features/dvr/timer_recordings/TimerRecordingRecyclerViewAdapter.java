package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TimerRecordingRecyclerViewAdapter extends RecyclerView.Adapter<TimerRecordingViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final boolean isDualPane;
    private final List<TimerRecording> recordingList = new ArrayList<>();
    private List<TimerRecording> recordingListFiltered = new ArrayList<>();
    private final int htspVersion;
    private final int gmtOffset;
    private int selectedPosition = 0;

    TimerRecordingRecyclerViewAdapter(boolean isDualPane, RecyclerViewClickCallback clickCallback, int htspVersion, int gmtOffset) {
        this.clickCallback = clickCallback;
        this.htspVersion = htspVersion;
        this.gmtOffset = gmtOffset;
        this.isDualPane = isDualPane;
    }

    @NonNull
    @Override
    public TimerRecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new TimerRecordingViewHolder(view, isDualPane);
    }

    @Override
    public void onBindViewHolder(@NonNull TimerRecordingViewHolder holder, int position) {
        if (recordingListFiltered.size() > position) {
            TimerRecording recording = recordingListFiltered.get(position);
            holder.bindData(recording, (selectedPosition == position), htspVersion, gmtOffset, clickCallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TimerRecordingViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<TimerRecording> list) {
        recordingList.clear();
        recordingListFiltered.clear();
        recordingList.addAll(list);
        recordingListFiltered.addAll(list);

        if (selectedPosition > list.size()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordingListFiltered != null ? recordingListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.timer_recording_list_adapter;
    }

    public void setPosition(int pos) {
        notifyItemChanged(selectedPosition);
        selectedPosition = pos;
        notifyItemChanged(pos);
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
}
