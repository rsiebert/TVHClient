package org.tvheadend.tvhclient.features.dvr.recordings;

import android.text.TextUtils;

import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import timber.log.Timber;

class RecordingListDiffCallback extends DiffUtil.Callback {

    static final int PAYLOAD_DATA_SIZE = 1;
    private static final int PAYLOAD_FULL = 2;

    private final List<Recording> oldList;
    private final List<Recording> newList;

    RecordingListDiffCallback(List<Recording> oldList, List<Recording> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return newList.get(newItemPosition).getId() == oldList.get(oldItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Recording oldRecording = oldList.get(oldItemPosition);
        Recording newRecording = newList.get(newItemPosition);

        return (newRecording.getId() == oldRecording.getId())
                && TextUtils.equals(newRecording.getTitle(), oldRecording.getTitle())
                && TextUtils.equals(newRecording.getSubtitle(), oldRecording.getSubtitle())
                && TextUtils.equals(newRecording.getSummary(), oldRecording.getSummary())
                && TextUtils.equals(newRecording.getDescription(), oldRecording.getDescription())
                && TextUtils.equals(newRecording.getChannelName(), oldRecording.getChannelName())
                && TextUtils.equals(newRecording.getAutorecId(), oldRecording.getAutorecId())
                && TextUtils.equals(newRecording.getTimerecId(), oldRecording.getTimerecId())
                && TextUtils.equals(newRecording.getDataErrors(), oldRecording.getDataErrors())

                && (newRecording.getStart() == oldRecording.getStart())
                && (newRecording.getStop() == oldRecording.getStop())
                && (newRecording.isEnabled() == oldRecording.isEnabled())
                && (newRecording.getDuplicate() == oldRecording.getDuplicate())
                && (newRecording.getDataSize() == oldRecording.getDataSize())

                && TextUtils.equals(newRecording.getError(), oldRecording.getError())
                && TextUtils.equals(newRecording.getState(), oldRecording.getState());
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Recording oldRecording = oldList.get(oldItemPosition);
        Recording newRecording = newList.get(newItemPosition);
        Timber.d("Checking payload for recording " + newRecording.getTitle());

        Timber.d("Recording " + newRecording.getTitle() + " datasize " + newRecording.getDataSize() + ", " + oldRecording.getDataSize());
        if (newRecording.getDataSize() != oldRecording.getDataSize()) {
            Timber.d("Recording " + newRecording.getTitle() + " datasize is " + newRecording.getDataSize());
            return PAYLOAD_DATA_SIZE;
        } else {
            return PAYLOAD_FULL;
        }
    }
}
