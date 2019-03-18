package org.tvheadend.tvhclient.ui.features.epg;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.EpgChannel;
import org.tvheadend.tvhclient.databinding.EpgChannelListAdapterBinding;
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class EpgChannelListRecyclerViewAdapter extends RecyclerView.Adapter<EpgChannelListRecyclerViewAdapter.EpgChannelViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final List<EpgChannel> channelList = new ArrayList<>();
    private List<EpgChannel> channelListFiltered = new ArrayList<>();

    EpgChannelListRecyclerViewAdapter(RecyclerViewClickCallback clickCallback) {
        this.clickCallback = clickCallback;
    }

    @NonNull
    @Override
    public EpgChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        EpgChannelListAdapterBinding itemBinding = EpgChannelListAdapterBinding.inflate(layoutInflater, parent, false);
        return new EpgChannelViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgChannelViewHolder holder, int position) {
        EpgChannel channel = channelListFiltered.get(position);
        holder.bind(channel, position, clickCallback);
    }

    void addItems(@NonNull List<EpgChannel> list) {
        channelList.clear();
        channelListFiltered.clear();
        channelList.addAll(list);
        channelListFiltered.addAll(list);

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return channelListFiltered != null ? channelListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_channel_list_adapter;
    }

    public EpgChannel getItem(int position) {
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
                    List<EpgChannel> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (EpgChannel channel : new CopyOnWriteArrayList<>(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (channel.getName() != null
                                && channel.getName().toLowerCase().contains(charString.toLowerCase())) {
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
                channelListFiltered = (ArrayList<EpgChannel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    static class EpgChannelViewHolder extends RecyclerView.ViewHolder {

        private final EpgChannelListAdapterBinding binding;

        EpgChannelViewHolder(EpgChannelListAdapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EpgChannel channel, int position, RecyclerViewClickCallback clickCallback) {
            binding.setChannel(channel);
            binding.setPosition(position);
            binding.setCallback(clickCallback);
            binding.executePendingBindings();
        }
    }
}
