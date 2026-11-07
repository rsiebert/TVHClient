package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import org.tvheadend.tvhclient.R

/**
 * Shows the state of the current subscription as reported by the server. This is the signal
 * quality of the tuner, the information about the descrambler (CAM) that is used and the
 * statistics of the queue that the server keeps for this subscription.
 */
class PlaybackStatusDialog : DialogFragment() {

    private var subscriptionState: HtspSubscriptionState = HtspSubscriptionState()

    init {
        // Retain the state across activity re-creation to prevent losing access to it.
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(requireContext(), R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
        dialog.setTitle(R.string.playback_status)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dialogView = inflater.inflate(R.layout.playback_status_dialog, container, false)
        val informationTextView = dialogView.findViewById<TextView>(R.id.playback_status_information)
        val okButton = dialogView.findViewById<Button>(R.id.playback_status_dialog_ok_button)
        informationTextView.text = getInformationText()
        okButton.setOnClickListener { dismiss() }
        return dialogView
    }

    private fun getInformationText(): String {
        val signalStatus = subscriptionState.signalStatus
        val descrambleInfo = subscriptionState.descrambleInfo
        val queueStatus = subscriptionState.queueStatus

        if (signalStatus == null && descrambleInfo == null && queueStatus == null && subscriptionState.status == null) {
            return getString(R.string.playback_status_unavailable)
        }

        val text = StringBuilder()

        subscriptionState.graceTimeout?.let {
            text.append(getString(R.string.htsp_status_grace_period, it)).append("\n\n")
        }
        subscriptionState.status?.let {
            text.append(getString(R.string.playback_status_error, it)).append("\n\n")
        }

        if (signalStatus != null) {
            text.append(getString(R.string.playback_status_signal)).append(":\n")
            appendRow(text, R.string.playback_status_signal_status, signalStatus.status)
            appendRow(text, R.string.playback_status_signal_strength, signalStatus.signal)
            appendRow(text, R.string.playback_status_signal_strength_absolute, signalStatus.absoluteSignal)
            appendRow(text, R.string.playback_status_signal_snr, signalStatus.snr)
            appendRow(text, R.string.playback_status_signal_snr_absolute, signalStatus.absoluteSnr)
            appendRow(text, R.string.playback_status_signal_ber, signalStatus.ber)
            appendRow(text, R.string.playback_status_signal_unc, signalStatus.unc)
        }

        if (descrambleInfo != null) {
            text.append("\n").append(getString(R.string.playback_status_descrambler)).append(":\n")
            appendRow(text, R.string.playback_status_card_system, descrambleInfo.cardSystem)
            appendRow(text, R.string.playback_status_reader, descrambleInfo.reader)
            appendRow(text, R.string.playback_status_from, descrambleInfo.from)
            appendRow(text, R.string.playback_status_protocol, descrambleInfo.protocol)
            appendRow(text, R.string.playback_status_pid, descrambleInfo.pid)
            appendRow(text, R.string.playback_status_caid, descrambleInfo.caid)
            appendRow(text, R.string.playback_status_provid, descrambleInfo.provid)
            appendRow(text, R.string.playback_status_hops, descrambleInfo.hops)
            appendRow(text, R.string.playback_status_ecm_time, descrambleInfo.ecmTime)
        }

        if (queueStatus != null) {
            text.append("\n").append(getString(R.string.playback_status_stream)).append(":\n")
            appendRow(text, R.string.playback_status_queue_packets, queueStatus.packets)
            appendRow(text, R.string.playback_status_queue_bytes, queueStatus.bytes)
            appendRow(text, R.string.playback_status_queue_errors, queueStatus.errors)
        }

        return text.toString().trim()
    }

    /**
     * Appends a "label: value" line to the text, unless the server did not provide the value.
     */
    private fun appendRow(text: StringBuilder, labelResId: Int, value: Any?) {
        if (value == null) {
            return
        }
        if (value is String && value.isEmpty()) {
            return
        }
        text.append("  ").append(getString(labelResId)).append(": ").append(value).append("\n")
    }

    companion object {
        fun create(state: HtspSubscriptionState): PlaybackStatusDialog {
            val playbackStatusDialog = PlaybackStatusDialog()
            playbackStatusDialog.subscriptionState = state
            return playbackStatusDialog
        }
    }
}
