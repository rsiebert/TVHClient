package org.tvheadend.tvhclient.features.epg;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.EpgProgram;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;

class EpgProgramListRecyclerViewAdapter extends RecyclerView.Adapter<EpgProgramListViewHolder> {

    private final RecyclerViewClickCallback clickCallback;
    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private final List<EpgProgram> programList = new ArrayList<>();
    private List<Recording> recordingList = new ArrayList<>();

    EpgProgramListRecyclerViewAdapter(float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime, @NonNull RecyclerViewClickCallback clickCallback) {
        this.clickCallback = clickCallback;
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
    }

    @NonNull
    @Override
    public EpgProgramListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new EpgProgramListViewHolder(view, pixelsPerMinute, fragmentStartTime, fragmentStopTime);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgProgramListViewHolder holder, int position) {
        if (programList.size() > position) {
            EpgProgram program = programList.get(position);
            holder.bindData(program, recordingList, clickCallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull EpgProgramListViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<EpgProgram> list) {
        updateRecordingState(list, recordingList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new EpgProgramListDiffCallback(programList, list));
        programList.clear();
        programList.addAll(list);
        diffResult.dispatchUpdatesTo(this);
    }

    void addRecordings(@NonNull List<Recording> list) {
        recordingList = list;
        updateRecordingState(programList, recordingList);
    }

    private void updateRecordingState(@NonNull List<EpgProgram> programs, @NonNull List<Recording> recordings) {
        for (int i = 0; i < programs.size(); i++) {
            EpgProgram program = programs.get(i);
            boolean recordingExists = false;

            for (Recording recording : recordings) {
                if (program.getEventId() == recording.getEventId()) {
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
}
