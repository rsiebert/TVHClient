package org.tvheadend.tvhclient.ui.features.dvr.recordings;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.databinding.RecordingListAdapterBinding;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingListDiffCallback.PAYLOAD_DATA_SIZE;

public class RecordingRecyclerViewAdapter extends RecyclerView.Adapter<RecordingRecyclerViewAdapter.RecordingViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final boolean isDualPane;
    private final List<Recording> recordingList = new ArrayList<>();
    private List<Recording> recordingListFiltered = new ArrayList<>();
    private final int htspVersion;
    private int selectedPosition = 0;

    RecordingRecyclerViewAdapter(boolean isDualPane, RecyclerViewClickCallback clickCallback, int htspVersion) {
        this.clickCallback = clickCallback;
        this.htspVersion = htspVersion;
        this.isDualPane = isDualPane;
    }

    @NonNull
    @Override
    public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecordingListAdapterBinding itemBinding = RecordingListAdapterBinding.inflate(layoutInflater, parent, false);
        return new RecordingViewHolder(itemBinding, isDualPane);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
        // NOP
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position, @NonNull List<Object> payloads) {
        Recording recording = recordingListFiltered.get(position);

        if (payloads.isEmpty()) {
            Timber.d("Recording '" + recording.getTitle() + "' has changed, doing a full update");
            holder.bind(recording, position, (selectedPosition == position), htspVersion, clickCallback);
        } else {
            for (final Object payload : payloads) {
                if (payload.equals(PAYLOAD_DATA_SIZE)) {
                    // Update only the data size and errors
                    Timber.d("Recording '" + recording.getTitle() + "' has changed, doing a partial update");
                    holder.bind(recording, position, (selectedPosition == position), htspVersion, clickCallback);
                }
            }
        }
    }

    void addItems(@NonNull List<Recording> newItems) {
        List<Recording> oldItems = new ArrayList<>(recordingListFiltered);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new RecordingListDiffCallback(oldItems, newItems));

        recordingList.clear();
        recordingListFiltered.clear();
        recordingList.addAll(newItems);
        recordingListFiltered.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);

        if (selectedPosition > recordingListFiltered.size()) {
            selectedPosition = 0;
        }
    }

    @Override
    public int getItemCount() {
        return recordingListFiltered != null ? recordingListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.recording_list_adapter;
    }

    public void setPosition(int pos) {
        notifyItemChanged(selectedPosition);
        selectedPosition = pos;
        notifyItemChanged(pos);
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
                    for (@NonNull Recording recording : new CopyOnWriteArrayList<>(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (recording.getTitle() != null
                                && recording.getTitle().toLowerCase().contains(charString.toLowerCase())) {
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

    static class RecordingViewHolder extends RecyclerView.ViewHolder {

        private final RecordingListAdapterBinding binding;
        private final boolean isDualPane;

        RecordingViewHolder(RecordingListAdapterBinding binding, boolean isDualPane) {
            super(binding.getRoot());
            this.binding = binding;
            this.isDualPane = isDualPane;
        }

        void bind(Recording recording, int position, boolean isSelected, int htspVersion, RecyclerViewClickCallback clickCallback) {
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
