package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EpgChannelListRecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private List<ChannelSubset> channelList = new ArrayList<>();
    private List<ChannelSubset> channelListFiltered = new ArrayList<>();
    private Context context;
    private int selectedPosition = 0;

    EpgChannelListRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.clickCallback = clickCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChannelSubset channel = channelListFiltered.get(position);
        ((EpgChannelViewHolder) holder).bindData(context, channel, clickCallback);
    }

    void addItems(List<ChannelSubset> list) {
        channelList.clear();
        channelListFiltered.clear();

        if (list != null) {
            channelList.addAll(list);
            channelListFiltered.addAll(list);
        }

        if (list == null || selectedPosition > list.size()) {
            selectedPosition = 0;
        }
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

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public ChannelSubset getItem(int position) {
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
                    List<ChannelSubset> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (ChannelSubset channel : new CopyOnWriteArrayList<>(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (channel.getName().toLowerCase().contains(charString.toLowerCase())) {
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
                channelListFiltered = (ArrayList<ChannelSubset>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
