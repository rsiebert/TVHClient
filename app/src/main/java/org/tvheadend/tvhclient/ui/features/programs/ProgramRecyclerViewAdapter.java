package org.tvheadend.tvhclient.ui.features.programs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.databinding.ProgramListAdapterBinding;
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class ProgramRecyclerViewAdapter extends RecyclerView.Adapter<ProgramRecyclerViewAdapter.ProgramViewHolder> implements Filterable {

    private final LastProgramVisibleListener onLastProgramVisibleListener;
    private final RecyclerViewClickCallback clickCallback;
    private final List<Program> programList = new ArrayList<>();
    private final boolean showProgramChannelIcon;
    private List<Program> programListFiltered = new ArrayList<>();
    private List<Recording> recordingList = new ArrayList<>();

    ProgramRecyclerViewAdapter(boolean showProgramChannelIcon, @NonNull RecyclerViewClickCallback clickCallback, @NonNull LastProgramVisibleListener onLastProgramVisibleListener) {
        this.clickCallback = clickCallback;
        this.onLastProgramVisibleListener = onLastProgramVisibleListener;
        this.showProgramChannelIcon = showProgramChannelIcon;
    }

    @NonNull
    @Override
    public ProgramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ProgramListAdapterBinding itemBinding = ProgramListAdapterBinding.inflate(layoutInflater, parent, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled));
        boolean showProgramSubtitles = sharedPreferences.getBoolean("program_subtitle_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_program_subtitle_enabled));

        return new ProgramViewHolder(itemBinding, showProgramChannelIcon, showGenreColors, showProgramSubtitles);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position) {
        if (programListFiltered.size() > position) {
            Program program = programListFiltered.get(position);
            holder.bind(program, position, clickCallback);
            if (position == programList.size() - 1) {
                onLastProgramVisibleListener.onLastProgramVisible(position);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<Program> newItems) {
        updateRecordingState(newItems, recordingList);

        List<Program> oldItems = new ArrayList<>(programListFiltered);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProgramListDiffCallback(oldItems, newItems));

        programList.clear();
        programListFiltered.clear();
        programList.addAll(newItems);
        programListFiltered.addAll(newItems);
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
                List<Program> filteredList = new ArrayList<>();
                if (charString.isEmpty()) {
                    filteredList = programList;
                } else {
                    // Iterate over the available program. Use a copy on write
                    // array in case the program list changes during filtering.
                    for (Program program : new CopyOnWriteArrayList<>(programList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (program.getTitle() != null
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
        recordingList.clear();
        recordingList.addAll(list);
        updateRecordingState(programListFiltered, recordingList);
    }

    private void updateRecordingState(@NonNull List<Program> programs, @NonNull List<Recording> recordings) {
        for (int i = 0; i < programs.size(); i++) {
            Program program = programs.get(i);
            boolean recordingExists = false;

            for (Recording recording : recordings) {
                if (program.getEventId() > 0 && program.getEventId() == recording.getEventId()) {
                    Recording oldRecording = program.getRecording();
                    program.setRecording(recording);

                    // Do a full update only when a new recording was added or the recording
                    // state has changed which results in a different recording state icon
                    // Otherwise do not update the UI
                    if (oldRecording == null
                            || (!TextUtils.equals(oldRecording.getError(), recording.getError())
                            || !TextUtils.equals(oldRecording.getState(), recording.getState()))) {
                        notifyItemChanged(i);
                    }
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

    static class ProgramViewHolder extends RecyclerView.ViewHolder {

        private final ProgramListAdapterBinding binding;
        private final boolean showProgramChannelIcon;
        private final boolean showGenreColors;
        private final boolean showProgramSubtitles;

        ProgramViewHolder(ProgramListAdapterBinding binding, boolean showProgramChannelIcon, boolean showGenreColors, boolean showProgramSubtitles) {
            super(binding.getRoot());
            this.binding = binding;
            this.showProgramChannelIcon = showProgramChannelIcon;
            this.showGenreColors = showGenreColors;
            this.showProgramSubtitles = showProgramSubtitles;
        }

        void bind(Program program, int position, RecyclerViewClickCallback clickCallback) {
            binding.setProgram(program);
            binding.setPosition(position);
            binding.setShowProgramSubtitles(showProgramSubtitles);
            binding.setShowProgramChannelIcon(showProgramChannelIcon);
            binding.setShowGenreColor(showGenreColors);
            binding.setCallback(clickCallback);
            binding.executePendingBindings();
        }
    }
}
