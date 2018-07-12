package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;

class EpgProgramListRecyclerViewAdapter extends RecyclerView.Adapter {

    private final RecyclerViewClickCallback clickCallback;
    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private Context context;
    private List<Program> programList = new ArrayList<>();

    EpgProgramListRecyclerViewAdapter(Context context, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgProgramListViewHolder(view, pixelsPerMinute, fragmentStartTime, fragmentStopTime);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Program program = programList.get(position);
        ((EpgProgramListViewHolder) holder).bindData(context, program, clickCallback);
    }

    void addItems(List<Program> list) {
        programList.clear();
        if (list != null) {
            programList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return programList != null ? programList.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        if (programList.get(position) == null) {
            return R.layout.epg_program_item_loading_adapter;
        } else {
            return R.layout.epg_program_item_adapter;
        }
    }

    public Program getItem(int position) {
        if (programList.size() > position && position >= 0) {
            return programList.get(position);
        } else {
            return null;
        }
    }
}
