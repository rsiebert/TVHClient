package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.programs.ProgramListDiffCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;

class EpgProgramListRecyclerViewAdapter extends RecyclerView.Adapter<EpgProgramListViewHolder> {

    private final RecyclerViewClickCallback clickCallback;
    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;
    private final Context context;
    private final List<Program> programList = new ArrayList<>();
    private List<Recording> recordingList = new ArrayList<>();

    EpgProgramListRecyclerViewAdapter(Context context, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime, RecyclerViewClickCallback clickCallback) {
        this.context = context;
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
        Program program = programList.get(position);
        holder.bindData(context, program, recordingList, clickCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgProgramListViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<Program> list) {
        updateRecordingState(list, recordingList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProgramListDiffCallback(programList, list));
        diffResult.dispatchUpdatesTo(this);

        programList.clear();
        programList.addAll(list);
    }

    void addRecordings(List<Recording> list) {
        recordingList = list;
        updateRecordingState(programList, recordingList);
    }

    private void updateRecordingState(List<Program> programs, List<Recording> recordings) {
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

    @Override
    public int getItemCount() {
        return programList.size();
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.epg_program_item_adapter;
    }

    public Program getItem(int position) {
        if (programList.size() > position && position >= 0) {
            return programList.get(position);
        } else {
            return null;
        }
    }
}
