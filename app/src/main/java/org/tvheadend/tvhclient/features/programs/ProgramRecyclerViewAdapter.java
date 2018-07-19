package org.tvheadend.tvhclient.features.programs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
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

import timber.log.Timber;

public class ProgramRecyclerViewAdapter extends RecyclerView.Adapter implements Filterable {

    private final BottomReachedCallback onBottomReachedCallback;
    private final RecyclerViewClickCallback clickCallback;
    private List<Program> programList = new ArrayList<>();
    private List<Program> programListFiltered = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Context context;

    ProgramRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback, BottomReachedCallback onBottomReachedCallback) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.onBottomReachedCallback = onBottomReachedCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ProgramViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Program program = programListFiltered.get(position);
        ((ProgramViewHolder) holder).bindData(context, program, clickCallback);
        if (position == programList.size() - 1) {
            onBottomReachedCallback.onBottomReached(position);
        }
    }

    void addItems(List<Program> list) {
        programList.clear();
        programListFiltered.clear();

        if (list != null) {
            programList.addAll(list);
            programListFiltered.addAll(list);
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProgramListDiffCallback(programList, list));
        diffResult.dispatchUpdatesTo(this);
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
                if (charString.isEmpty()) {
                    programListFiltered = programList;
                } else {
                    List<Program> filteredList = new ArrayList<>();
                    // Iterate over the available program. Use a copy on write
                    // array in case the program list changes during filtering.
                    for (Program program : new CopyOnWriteArrayList<>(programList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (program.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(program);
                        }
                    }
                    programListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = programListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                Timber.d("Done filtering");
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
     * @param recordings List of recordings
     */
    void addRecordings(List<Recording> recordings) {
        for (Recording recording : recordings) {
            for (int i = 0; i < programList.size(); i++) {
                Program program = programList.get(i);
                if (recording.getEventId() == program.getEventId()) {
                    program.setRecording(recording);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }
}
