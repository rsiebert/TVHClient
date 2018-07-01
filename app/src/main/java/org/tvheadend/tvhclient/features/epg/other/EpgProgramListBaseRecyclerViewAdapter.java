package org.tvheadend.tvhclient.features.epg.other;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.List;

import timber.log.Timber;

public class EpgProgramListBaseRecyclerViewAdapter extends RecyclerView.Adapter {

    private final RecyclerViewClickCallback clickCallback;
    private final int displayWidth;
    private final int gridLayoutColumnCount;
    private final int recyclerViewTotalWidth;
    private SparseArray<List<Program>> programList = new SparseArray<>();
    private SparseArray<Channel> channelList = new SparseArray<>();
    private Context context;

    EpgProgramListBaseRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback, int displayWidth) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.displayWidth = displayWidth;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int hoursToShow = Integer.valueOf(sharedPreferences.getString("hours_of_epg_data_per_screen", "4"));
        int daysToShow = 1;//Integer.valueOf(sharedPreferences.getString("days_of_epg_data", "7"));

        gridLayoutColumnCount = hoursToShow * 60 * daysToShow;
        Timber.d("Showing " + (hoursToShow * 60) + " minutes on the screen, total minutes " + gridLayoutColumnCount);

        // The width of the visible part of the grid layout is screen width - 90dp. (hoursToShow * 60)
        Timber.d("Total display width is " + displayWidth);
        int gridScreenWidth = displayWidth - 90;
        Timber.d("Grid screen width is " + gridScreenWidth);
        int gridSingleColumnWidth = (gridScreenWidth / (hoursToShow * 60));
        Timber.d("Grid single column width is " + gridSingleColumnWidth);
        recyclerViewTotalWidth = gridSingleColumnWidth * gridLayoutColumnCount;
        Timber.d("Grid total column width is " + recyclerViewTotalWidth);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);

        Timber.d("onCreateViewHolder Setting width to " + recyclerViewTotalWidth);
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
        layoutParams.width = recyclerViewTotalWidth;
        view.setLayoutParams(layoutParams);
        Timber.d("onCreateViewHolder Set width to " + recyclerViewTotalWidth);

        return new EpgProgramListBaseViewHolder(view, clickCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        List<Program> programs = programList.get(position);
        Channel channel = channelList.get(position);
        Timber.d("Getting programs for channel " + (channel != null ? channel.getName() : "null") + " at adapter position " + position);
        ((EpgProgramListBaseViewHolder) holder).bindData(context, channel, programs, gridLayoutColumnCount, clickCallback);
    }

    void addItems(int position, Channel channel, List<Program> list) {
        Timber.d("Adding programs for channel " + channel.getName() + " at position " + position);
        programList.remove(position);

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
