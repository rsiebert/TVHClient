package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.tvheadend.tvhclient.R
import java.util.*


class TrackInformationDialog : DialogFragment() {

    private var videoFormat: Format? = null
    private var audioFormat: Format? = null
    private var playWhenReady: Boolean = false
    private var playbackState = 0
    private var titleId = 0

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }

    private fun init(player: ExoPlayer) {
        titleId = R.string.pref_information
        videoFormat = player.videoFormat
        audioFormat = player.audioFormat
        playbackState = player.playbackState
        playWhenReady = player.playWhenReady
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
        dialog.setTitle(titleId)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dialogView = inflater.inflate(R.layout.track_information_dialog, container, false)
        val informationTextView = dialogView.findViewById<TextView>(R.id.track_information)
        val okButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button)
        informationTextView.text = getInformationText()
        okButton.setOnClickListener { dismiss() }
        return dialogView
    }

    private fun getInformationText(): String {
        val audioInfo = StringBuilder()
                .appendRow(R.string.track_information_mime_type, audioFormat?.sampleMimeType)
                .appendRow(R.string.track_information_id, audioFormat?.id)
                .appendRow(R.string.track_information_sample_rate, audioFormat?.sampleRate)
                .appendRow(R.string.track_information_channel_count, audioFormat?.channelCount)

        val videoInfo = StringBuilder()
                .appendRow(R.string.track_information_mime_type, videoFormat?.sampleMimeType)
                .appendRow(R.string.track_information_id, videoFormat?.id)
                .appendRow(R.string.track_information_width, videoFormat?.width)
                .appendRow(R.string.track_information_height, videoFormat?.height)
                .appendRow(R.string.track_information_aspect_ratio, getPixelAspectRatio(videoFormat?.pixelWidthHeightRatio))

        return StringBuilder()
                .append(getString(R.string.track_information_play_when_ready)).append(": ").append(playWhenReady).append("\n\n")
                .append(getString(R.string.track_information_playback_state)).append(": ").append(getPlayerStateString()).append("\n\n")
                .append(getString(R.string.exo_track_selection_title_video)).append(":\n")
                .append(videoInfo)
                .append("\n")
                .append(getString(R.string.exo_track_selection_title_audio)).append(":\n")
                .append(audioInfo)
                .toString()
    }

    /**
     * Appends a "  label: value" line for the given label, or nothing at all when the player did
     * not report a value for it.
     */
    private fun StringBuilder.appendRow(labelResId: Int, value: Any?): StringBuilder {
        if (value == null) {
            return this
        }
        if (value is String && value.isEmpty()) {
            return this
        }
        return append("  ").append(getString(labelResId)).append(": ").append(value).append("\n")
    }

    private fun getPlayerStateString(): String {
        return when (playbackState) {
            Player.STATE_BUFFERING -> getString(R.string.track_information_state_buffering)
            Player.STATE_ENDED -> getString(R.string.track_information_state_ended)
            Player.STATE_IDLE -> getString(R.string.track_information_state_idle)
            Player.STATE_READY -> getString(R.string.track_information_state_ready)
            else -> getString(R.string.track_information_state_unknown)
        }
    }

    /**
     * The pixel aspect ratio with two decimals, or the "no value" placeholder when the player did
     * not report one. Returning a non-null value keeps the aspect ratio row in the video block.
     */
    private fun getPixelAspectRatio(pixelAspectRatio: Float?): String {
        if (pixelAspectRatio == null || pixelAspectRatio == Format.NO_VALUE.toFloat()) {
            return getString(R.string.track_information_no_value)
        }
        return String.format(Locale.US, "%.02f", pixelAspectRatio)
    }

    companion object {
        fun createForTrackSelector(player: ExoPlayer): TrackInformationDialog {
            val trackInformationDialog = TrackInformationDialog()
            trackInformationDialog.init(player)
            return trackInformationDialog
        }
    }
}