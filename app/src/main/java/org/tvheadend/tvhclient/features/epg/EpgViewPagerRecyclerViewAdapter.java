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
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpgViewPagerRecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private final FragmentActivity activity;
    private Map<Integer, List<Program>> programList = new HashMap<>();
    private Map<Integer, List<Program>> filteredProgramList = new HashMap<>();
    private Map<Integer, List<Recording>> recordingList = new HashMap<>();

    EpgViewPagerRecyclerViewAdapter(FragmentActivity activity, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime) {
        this.activity = activity;
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgViewPagerViewHolder(activity, view, pixelsPerMinute, fragmentStartTime, fragmentStopTime);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        List<Program> programs = filteredProgramList.get(position);
        List<Recording> recordings = recordingList.get(position);
        ((EpgViewPagerViewHolder) holder).bindData(programs, recordings);
    }

    void addItems(int position, List<Program> list) {
        programList.remove(position);
        filteredProgramList.remove(position);
        if (list != null) {
            programList.put(position, list);
            filteredProgramList.put(position, list);
        }
        notifyDataSetChanged();
    }

    void addRecordings(int position, List<Recording> list) {
        recordingList.remove(position);
        if (list != null) {
            recordingList.put(position, list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return filteredProgramList != null ? filteredProgramList.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_program_list_adapter;
    }

    public void clearItems() {
        programList.clear();
        filteredProgramList.clear();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filteredProgramList = programList;
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
                    filteredProgramList = newFilteredProgramList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredProgramList;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredProgramList = (HashMap<Integer, List<Program>>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
