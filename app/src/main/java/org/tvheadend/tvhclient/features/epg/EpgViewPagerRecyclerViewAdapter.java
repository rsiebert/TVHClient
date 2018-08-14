package org.tvheadend.tvhclient.features.epg;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;

import java.util.ArrayList;
import java.util.List;

public class EpgViewPagerRecyclerViewAdapter extends RecyclerView.Adapter<EpgViewPagerViewHolder> implements Filterable {

    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private final FragmentActivity activity;
    private final RecyclerView.RecycledViewPool viewPool;
    private List<ChannelSubset> channelList = new ArrayList<>();
    private List<ChannelSubset> channelListFiltered = new ArrayList<>();

    EpgViewPagerRecyclerViewAdapter(FragmentActivity activity, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime) {
        this.activity = activity;
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
        this.viewPool = new RecyclerView.RecycledViewPool();;
    }

    @NonNull
    @Override
    public EpgViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgViewPagerViewHolder(activity, view, pixelsPerMinute, fragmentStartTime, fragmentStopTime, viewPool);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgViewPagerViewHolder holder, int position) {
        ChannelSubset channelSubset = channelListFiltered.get(position);
        holder.bindData(channelSubset);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgViewPagerViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
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
        return null;
/*
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    programListFiltered = programList;
                } else {
                    HashMap<Integer, List<Program>> newFilteredProgramList = new HashMap<>();

                    for (Map.Entry<Integer, List<Program>> entry : programList.entrySet()) {
                        List<Program> programListForOneChannel = new ArrayList<>();
                        for (Program program : entry.getValue()) {
                            if (program.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                                programListForOneChannel.add(program);
                            }
                        }
                        newFilteredProgramList.put(entry.getKey(), programListForOneChannel);
                    }
                    programListFiltered = newFilteredProgramList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = programListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                programListFiltered = (HashMap<Integer, List<Program>>) filterResults.values;
                notifyDataSetChanged();
            }
        };
*/
    }

    public void addChannelItems(List<ChannelSubset> channels) {
        channelList.clear();
        channelListFiltered.clear();

        channelList.addAll(channels);
        channelListFiltered.addAll(channels);

        notifyDataSetChanged();
    }
}
