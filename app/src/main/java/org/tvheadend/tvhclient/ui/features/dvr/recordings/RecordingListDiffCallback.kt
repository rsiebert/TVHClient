package org.tvheadend.tvhclient.ui.features.dvr.recordings

import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.util.isEqualTo
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
                && newRecording.title.isEqualTo(oldRecording.title)
                && newRecording.subtitle.isEqualTo(oldRecording.subtitle)
                && newRecording.summary.isEqualTo(oldRecording.summary)
                && newRecording.description.isEqualTo(oldRecording.description)
                && newRecording.channelName.isEqualTo(oldRecording.channelName)
                && newRecording.autorecId.isEqualTo(oldRecording.autorecId)
                && newRecording.timerecId.isEqualTo(oldRecording.timerecId)
                && newRecording.dataErrors.isEqualTo(oldRecording.dataErrors)

                && newRecording.start == oldRecording.start
                && newRecording.stop == oldRecording.stop
                && newRecording.isEnabled == oldRecording.isEnabled
                && newRecording.duplicate == oldRecording.duplicate
                && newRecording.dataSize == oldRecording.dataSize

                && newRecording.error.isEqualTo(oldRecording.error)
                && newRecording.state.isEqualTo(oldRecording.state))
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