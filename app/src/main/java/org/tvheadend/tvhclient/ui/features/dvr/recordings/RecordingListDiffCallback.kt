package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.text.TextUtils
import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.tvhclient.domain.entity.Recording
import timber.log.Timber

class RecordingListDiffCallback(private var oldList: List<Recording>, private var newList: List<Recording>) : DiffUtil.Callback() {

    companion object {
        const val PAYLOAD_DATA_SIZE = 1
        const val PAYLOAD_FULL = 2
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].id == oldList[oldItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldRecording = oldList[oldItemPosition]
        val newRecording = newList[newItemPosition]

        return (newRecording.id == oldRecording.id
                && TextUtils.equals(newRecording.title, oldRecording.title)
                && TextUtils.equals(newRecording.subtitle, oldRecording.subtitle)
                && TextUtils.equals(newRecording.summary, oldRecording.summary)
                && TextUtils.equals(newRecording.description, oldRecording.description)
                && TextUtils.equals(newRecording.channelName, oldRecording.channelName)
                && TextUtils.equals(newRecording.autorecId, oldRecording.autorecId)
                && TextUtils.equals(newRecording.timerecId, oldRecording.timerecId)
                && TextUtils.equals(newRecording.dataErrors, oldRecording.dataErrors)

                && newRecording.start == oldRecording.start
                && newRecording.stop == oldRecording.stop
                && newRecording.isEnabled == oldRecording.isEnabled
                && newRecording.duplicate == oldRecording.duplicate
                && newRecording.dataSize == oldRecording.dataSize

                && TextUtils.equals(newRecording.error, oldRecording.error)
                && TextUtils.equals(newRecording.state, oldRecording.state))
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldRecording = oldList[oldItemPosition]
        val newRecording = newList[newItemPosition]

        Timber.d("Checking payload for recording ${newRecording.title} data size ${newRecording.dataSize}, ${oldRecording.dataSize}")

        return if (newRecording.dataSize != oldRecording.dataSize) {
            Timber.d("Recording data size has changed only")
            PAYLOAD_DATA_SIZE
        } else {
            Timber.d("Recording has changed")
            PAYLOAD_FULL
        }
    }
}