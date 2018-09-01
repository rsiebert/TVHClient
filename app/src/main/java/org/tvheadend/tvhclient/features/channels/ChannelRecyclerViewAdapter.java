package org.tvheadend.tvhclient.features.channels;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ChannelRecyclerViewAdapter extends RecyclerView.Adapter<ChannelViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final boolean isDualPane;
    private List<Recording> recordingList = new ArrayList<>();
    private final List<Channel> channelList = new ArrayList<>();
    private List<Channel> channelListFiltered = new ArrayList<>();
    private final Context context;
    private int selectedPosition = 0;

    ChannelRecyclerViewAdapter(Context context, boolean isDualPane, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.isDualPane = isDualPane;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelListFiltered.get(position);
        holder.bindData(context, channel, (selectedPosition == position), clickCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<Channel> list) {
        updateRecordingState(list, recordingList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChannelListDiffCallback(channelList, list));
        diffResult.dispatchUpdatesTo(this);

        channelList.clear();
        channelListFiltered.clear();
        channelList.addAll(list);
        channelListFiltered.addAll(list);

        if (selectedPosition > list.size()) {
            selectedPosition = 0;
        }
    }

    @Override
    public int getItemCount() {
        return channelListFiltered != null ? channelListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return isDualPane ? R.layout.channel_list_adapter_dualpane : R.layout.channel_list_adapter;
    }

    public void setPosition(int pos) {
        notifyItemChanged(selectedPosition);
        selectedPosition = pos;
        notifyItemChanged(pos);
    }

    public Channel getItem(int position) {
        if (channelListFiltered.size() > position && position >= 0) {
            return channelListFiltered.get(position);
        } else {
            return null;
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    channelListFiltered = channelList;
                } else {
                    List<Channel> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (Channel channel : new CopyOnWriteArrayList<>(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (channel.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramTitle() != null
                                && channel.getProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramSubtitle() != null
                                && channel.getProgramSubtitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getNextProgramTitle() != null
                                && channel.getNextProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        }
                    }
                    channelListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = channelListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                channelListFiltered = (ArrayList<Channel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    /**
     * Whenever a recording changes in the database the list of available recordings are
     * saved in this recycler view. The previous list is cleared to avoid showing outdated
     * recording states. Each recording is checked if it belongs to the
     * currently shown program. If yes then its state is updated.
     *
     * @param list List of recordings
     */
    void addRecordings(@NonNull List<Recording> list) {
        recordingList = list;
        updateRecordingState(channelList, recordingList);
    }

    private void updateRecordingState(@NonNull List<Channel> channels, @NonNull List<Recording> recordings) {
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            boolean recordingExists = false;

            for (Recording recording : recordings) {
                if (channel.getProgramId() == recording.getEventId()) {
                    channel.setRecording(recording);
                    notifyItemChanged(i);
                    recordingExists = true;
                    break;
                }
            }
            if (!recordingExists && channel.getRecording() != null) {
                channel.setRecording(null);
                notifyItemChanged(i);
            }
            channels.set(i, channel);
        }
    }
}
