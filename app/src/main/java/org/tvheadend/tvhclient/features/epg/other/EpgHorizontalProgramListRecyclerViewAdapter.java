package org.tvheadend.tvhclient.features.epg.other;

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

import timber.log.Timber;

class EpgHorizontalProgramListRecyclerViewAdapter extends RecyclerView.Adapter {

    private final RecyclerViewClickCallback clickCallback;
    private Context context;
    private List<Program> programList = new ArrayList<>();

    EpgHorizontalProgramListRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.clickCallback = clickCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgHorizontalProgramListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Program program = programList.get(position);
        ((EpgHorizontalProgramListViewHolder) holder).bindData(context, program, clickCallback);
    }

    void addItems(List<Program> list) {
        Timber.d("Adding programs");
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
        return R.layout.epg_program_item_adapter;
    }
}
