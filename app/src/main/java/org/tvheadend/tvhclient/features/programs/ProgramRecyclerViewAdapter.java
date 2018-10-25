package org.tvheadend.tvhclient.features.programs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.callbacks.BottomReachedCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ProgramRecyclerViewAdapter extends RecyclerView.Adapter<ProgramViewHolder> implements Filterable {

    private final BottomReachedCallback onBottomReachedCallback;
    private final RecyclerViewClickCallback clickCallback;
    private final List<Program> programList = new ArrayList<>();
    private final boolean showProgramChannelIcon;
    private List<Program> programListFiltered = new ArrayList<>();
    private List<Recording> recordingList = new ArrayList<>();

    ProgramRecyclerViewAdapter(@NonNull RecyclerViewClickCallback clickCallback, @Nullable BottomReachedCallback onBottomReachedCallback, boolean showProgramChannelIcon) {
        this.clickCallback = clickCallback;
        this.onBottomReachedCallback = onBottomReachedCallback;
        this.showProgramChannelIcon = showProgramChannelIcon;
    }

    @NonNull
    @Override
    public ProgramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ProgramViewHolder(view, showProgramChannelIcon);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position) {
        if (programListFiltered.size() > position) {
            Program program = programListFiltered.get(position);
            holder.bindData(program, clickCallback);
            if (position == programList.size() - 1
                    && onBottomReachedCallback != null) {
                onBottomReachedCallback.onBottomReached(position);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<Program> list) {
        updateRecordingState(list, recordingList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProgramListDiffCallback(programList, list));
        diffResult.dispatchUpdatesTo(this);

        programList.clear();
        programListFiltered.clear();
        programList.addAll(list);
        programListFiltered.addAll(list);
    }

    @Override
    public int getItemCount() {
        return programListFiltered != null ? programListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.program_list_adapter;
    }

    public Program getItem(int position) {
        if (programListFiltered.size() > position && position >= 0) {
            return programListFiltered.get(position);
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
                List<Program> filteredList = new ArrayList<>();
                if (charString.isEmpty()) {
                    filteredList = programList;
                } else {
                    // Iterate over the available program. Use a copy on write
                    // array in case the program list changes during filtering.
                    for (Program program : new CopyOnWriteArrayList<>(programList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (!TextUtils.isEmpty(program.getTitle())
                                && program.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(program);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                programListFiltered = (ArrayList<Program>) filterResults.values;
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
        updateRecordingState(programList, recordingList);
    }

    private void updateRecordingState(@NonNull List<Program> programs, @NonNull List<Recording> recordings) {
        for (int i = 0; i < programs.size(); i++) {
            Program program = programs.get(i);
            boolean recordingExists = false;

            for (Recording recording : recordings) {
                if (program.getEventId() == recording.getEventId()) {
                    program.setRecording(recording);
                    notifyItemChanged(i);
                    recordingExists = true;
                    break;
                }
            }
            if (!recordingExists && program.getRecording() != null) {
                program.setRecording(null);
                notifyItemChanged(i);
            }
            programs.set(i, program);
        }
    }
}
