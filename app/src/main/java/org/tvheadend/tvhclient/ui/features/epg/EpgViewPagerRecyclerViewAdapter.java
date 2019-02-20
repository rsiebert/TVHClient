package org.tvheadend.tvhclient.ui.features.epg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.EpgChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

class EpgViewPagerRecyclerViewAdapter extends RecyclerView.Adapter<EpgViewPagerViewHolder> implements Filterable {

    private final float pixelsPerMinute;
    private final long startTime;
    private final long endTime;
    private final FragmentActivity activity;
    private final RecyclerView.RecycledViewPool viewPool;
    private final List<EpgChannel> channelList = new ArrayList<>();
    private List<EpgChannel> channelListFiltered = new ArrayList<>();

    EpgViewPagerRecyclerViewAdapter(FragmentActivity activity, float pixelsPerMinute, long startTime, long endTime) {
        this.activity = activity;
        this.pixelsPerMinute = pixelsPerMinute;
        this.startTime = startTime;
        this.endTime = endTime;
        this.viewPool = new RecyclerView.RecycledViewPool();
    }

    @NonNull
    @Override
    public EpgViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgViewPagerViewHolder(activity, view, pixelsPerMinute, startTime, endTime, viewPool);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgViewPagerViewHolder holder, int position) {
        EpgChannel epgChannel = channelListFiltered.get(position);
        holder.bindData(epgChannel);
    }

    @Override
    public int getItemCount() {
        return channelListFiltered != null ? channelListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_program_list_adapter;
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
                    for (@NonNull EpgChannel channel : new CopyOnWriteArrayList<>(channelList)) {
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

    public void addItems(@NonNull List<EpgChannel> channels) {
        channelList.clear();
        channelListFiltered.clear();
        channelList.addAll(channels);
        channelListFiltered.addAll(channels);
        notifyDataSetChanged();
    }
}
