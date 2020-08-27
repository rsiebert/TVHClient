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
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
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

    private fun init(player: SimpleExoPlayer) {
        // TODO change variable string
        titleId = R.string.pref_information
        videoFormat = player.videoFormat
        audioFormat = player.audioFormat
        playbackState = player.playbackState
        playWhenReady = player.playWhenReady
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
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
        val audioInfo = "  Mime type: ${audioFormat?.sampleMimeType}\n" +
                "  Id: ${audioFormat?.id}\n" +
                "  Sample rate: ${audioFormat?.sampleRate}\n" +
                "  Channel count: ${audioFormat?.channelCount}\n"

        val videoInfo = "  Mime type: ${videoFormat?.sampleMimeType}\n" +
                "  Id: ${videoFormat?.id}\n" +
                "  Width: ${videoFormat?.width}\n" +
                "  Height: ${videoFormat?.height}\n" +
                "  Aspect ratio: ${getPixelAspectRatioString(videoFormat?.pixelWidthHeightRatio)}\n"

        return "Play when ready: $playWhenReady\n\n" +
                "Playback state: ${getPlayerStateString()}\n\n" +
                "Video:\n$videoInfo\n" +
                "Audio:\n$audioInfo"
    }

    private fun getPlayerStateString(): String? {
        return when (playbackState) {
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_ENDED -> "ended"
            Player.STATE_IDLE -> "idle"
            Player.STATE_READY -> "ready"
            else -> "unknown"
        }
    }

    private fun getPixelAspectRatioString(pixelAspectRatio: Float?): String? {
        return if (pixelAspectRatio == Format.NO_VALUE.toFloat()) " par: no value" else " par:" + java.lang.String.format(Locale.US, "%.02f", pixelAspectRatio)
    }

    companion object {
        fun createForTrackSelector(player: SimpleExoPlayer): TrackInformationDialog {
            val trackInformationDialog = TrackInformationDialog()
            trackInformationDialog.init(player)
            return trackInformationDialog
        }
    }
}