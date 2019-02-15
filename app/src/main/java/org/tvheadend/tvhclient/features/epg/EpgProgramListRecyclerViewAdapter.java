package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.EpgProgram;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.databinding.EpgProgramItemAdapterBinding;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

class EpgProgramListRecyclerViewAdapter extends RecyclerView.Adapter<EpgProgramListRecyclerViewAdapter.EpgProgramListViewHolder> implements RecyclerViewClickCallback {

    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private final List<EpgProgram> programList = new ArrayList<>();
    private List<Recording> recordingList = new ArrayList<>();

    EpgProgramListRecyclerViewAdapter(float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime) {
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
    }

    @NonNull
    @Override
    public EpgProgramListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        EpgProgramItemAdapterBinding itemBinding = EpgProgramItemAdapterBinding.inflate(layoutInflater, parent, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_program_subtitle_enabled));
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_program_guide_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_genre_colors_for_program_guide_enabled));

        return new EpgProgramListViewHolder(itemBinding, showProgramSubtitle, showGenreColors, this);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgProgramListViewHolder holder, int position) {
        if (programList.size() > position) {
            EpgProgram program = programList.get(position);
            for (Recording rec : recordingList) {
                if (rec.getEventId() == program.getEventId()) {
                    program.setRecording(rec);
                    break;
                }
            }

            long startTime = (program.getStart() < fragmentStartTime) ? fragmentStartTime : program.getStart();
            long stopTime = (program.getStop() > fragmentStopTime) ? fragmentStopTime : program.getStop();
            int layoutWidth = (int) (((stopTime - startTime) / 1000 / 60) * pixelsPerMinute);

            holder.bind(program, position, layoutWidth);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull EpgProgramListViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<EpgProgram> newItems) {
        updateRecordingState(newItems, recordingList);

        List<EpgProgram> oldItems = new ArrayList<>(programList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new EpgProgramListDiffCallback(oldItems, newItems));

        programList.clear();
        programList.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    void addRecordings(@NonNull List<Recording> list) {
        recordingList.clear();
        recordingList.addAll(list);
        updateRecordingState(programList, recordingList);
    }

    private void updateRecordingState(@NonNull List<EpgProgram> programs, @NonNull List<Recording> recordings) {
        for (int i = 0; i < programs.size(); i++) {
            EpgProgram program = programs.get(i);
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

    @Override
    public int getItemCount() {
        return programList.size();
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_program_item_adapter;
    }

    public EpgProgram getItem(int position) {
        if (programList.size() > position && position >= 0) {
            return programList.get(position);
        } else {
            return null;
        }
    }

    @Override
    public void onClick(View view, int position) {
        EpgProgram program = getItem(position);
        if (program == null) {
            return;
        }
        Intent intent = new Intent(view.getContext(), ProgramDetailsActivity.class);
        intent.putExtra("eventId", program.getEventId());
        intent.putExtra("channelId", program.getChannelId());
        view.getContext().startActivity(intent);
    }

    @Override
    public boolean onLongClick(View view, int position) {
        EpgProgram program = getItem(position);
        if (program == null) {
            return false;
        }

        // Get the activity from the view context so the fragment manager can be accessed
        AppCompatActivity activity = null;
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                activity = (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
            if (fragment instanceof ProgramGuideFragment
                    && fragment.isAdded()
                    && fragment.isResumed()) {
                ((ProgramGuideFragment) fragment).showPopupMenu(view, program);
            }
        }
        return true;
    }

    public static class EpgProgramListViewHolder extends RecyclerView.ViewHolder {

        private final EpgProgramItemAdapterBinding binding;
        private final boolean showProgramSubtitle;
        private final boolean showGenreColors;
        private final RecyclerViewClickCallback clickCallback;

        EpgProgramListViewHolder(EpgProgramItemAdapterBinding binding, boolean showProgramSubtitle, boolean showGenreColors, RecyclerViewClickCallback clickCallback) {
            super(binding.getRoot());
            this.binding = binding;
            this.showProgramSubtitle = showProgramSubtitle;
            this.showGenreColors = showGenreColors;
            this.clickCallback = clickCallback;
        }

        public void bind(EpgProgram program, int position, int layoutWidth) {
            binding.setProgram(program);
            binding.setPosition(position);
            binding.setLayoutWidth(layoutWidth);
            binding.setShowGenreColor(showGenreColors);
            binding.setShowProgramSubtitle(showProgramSubtitle);
            binding.setCallback(clickCallback);
            binding.executePendingBindings();
        }
    }
}
