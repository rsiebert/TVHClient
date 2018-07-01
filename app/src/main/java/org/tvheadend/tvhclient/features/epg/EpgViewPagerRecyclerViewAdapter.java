package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.Program;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class EpgViewPagerRecyclerViewAdapter extends RecyclerView.Adapter {

    private Map<Integer, List<Program>> programList = new HashMap<>();
    private Map<Integer, ChannelSubset> channelList = new HashMap<>();
    private final Context context;

    EpgViewPagerRecyclerViewAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgViewPagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        List<Program> programs = programList.get(position);
        ChannelSubset channel = channelList.get(position);
        Timber.d("Getting " + (programs != null ? programs.size() : 0) + " programs for channel " + (channel != null ? channel.getName() : "null") + " at adapter position " + position);
        ((EpgViewPagerViewHolder) holder).bindData(context, channel, programs);
    }

    void addItems(int position, ChannelSubset channel, List<Program> list) {
        Timber.d("Adding " + (list != null ? list.size() : 0) + " programs for channel " + channel.getName() + " at position " + position);
        programList.remove(position);
        channelList.remove(position);

        channelList.put(position, channel);
        if (list != null) {
            programList.put(position, list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return programList != null ? programList.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_program_list_adapter;
    }
}
