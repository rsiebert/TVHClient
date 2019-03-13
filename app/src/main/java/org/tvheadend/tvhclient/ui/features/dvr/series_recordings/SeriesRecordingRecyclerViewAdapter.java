package org.tvheadend.tvhclient.ui.features.dvr.series_recordings;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.databinding.SeriesRecordingListAdapterBinding;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class SeriesRecordingRecyclerViewAdapter extends RecyclerView.Adapter<SeriesRecordingRecyclerViewAdapter.SeriesRecordingViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final boolean isDualPane;
    private final List<SeriesRecording> recordingList = new ArrayList<>();
    private List<SeriesRecording> recordingListFiltered = new ArrayList<>();
    private final int htspVersion;
    private int selectedPosition = 0;

    SeriesRecordingRecyclerViewAdapter(boolean isDualPane, RecyclerViewClickCallback clickCallback, int htspVersion) {
        this.clickCallback = clickCallback;
        this.htspVersion = htspVersion;
        this.isDualPane = isDualPane;
    }

    @NonNull
    @Override
    public SeriesRecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        SeriesRecordingListAdapterBinding itemBinding = SeriesRecordingListAdapterBinding.inflate(layoutInflater, parent, false);
        return new SeriesRecordingViewHolder(itemBinding, isDualPane);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesRecordingViewHolder holder, int position) {
        if (recordingListFiltered.size() > position) {
            SeriesRecording recording = recordingListFiltered.get(position);
            holder.bind(recording, position, (selectedPosition == position), htspVersion, clickCallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesRecordingViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<SeriesRecording> newItems) {
        recordingList.clear();
        recordingListFiltered.clear();
        recordingList.addAll(newItems);
        recordingListFiltered.addAll(newItems);

        if (selectedPosition > recordingListFiltered.size()) {
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
        return R.layout.series_recording_list_adapter;
    }

    public void setPosition(int pos) {
        notifyItemChanged(selectedPosition);
        selectedPosition = pos;
        notifyItemChanged(pos);
    }

    public SeriesRecording getItem(int position) {
        if (recordingListFiltered.size() > position && position >= 0) {
            return recordingListFiltered.get(position);
        } else {
            return null;
        }
    }

    public List<SeriesRecording> getItems() {
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
                    List<SeriesRecording> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (@NonNull SeriesRecording recording : new CopyOnWriteArrayList<>(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (recording.getTitle() != null
                                && recording.getTitle().toLowerCase().contains(charString.toLowerCase())) {
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
                recordingListFiltered = (ArrayList<SeriesRecording>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    static class SeriesRecordingViewHolder extends RecyclerView.ViewHolder {

        private final SeriesRecordingListAdapterBinding binding;
        private final boolean isDualPane;

        SeriesRecordingViewHolder(SeriesRecordingListAdapterBinding binding, boolean isDualPane) {
            super(binding.getRoot());
            this.binding = binding;
            this.isDualPane = isDualPane;
        }

        void bind(SeriesRecording recording, int position, boolean isSelected, int htspVersion, RecyclerViewClickCallback clickCallback) {
            binding.setRecording(recording);
            binding.setPosition(position);
            binding.setHtspVersion(htspVersion);
            binding.setIsSelected(isSelected);
            binding.setIsDualPane(isDualPane);
            binding.setCallback(clickCallback);
            binding.executePendingBindings();
        }
    }
}
