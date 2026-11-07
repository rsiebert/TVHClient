package org.tvheadend.data.entity

/**
 * A range of a recording that the server marked, for example a commercial break.
 *
 * The start and end positions are milliseconds relative to the beginning of the recording.
 * The values are provided by the server in the reply to the `getDvrCutpoints` request.
 */
data class DvrCutpoint(

        // Start of the range in milliseconds relative to the beginning of the recording
        var start: Long = 0,

        // End of the range in milliseconds relative to the beginning of the recording
        var end: Long = 0,

        // Type of the range, see the TYPE_ constants
        var type: Int = TYPE_UNKNOWN
) {

    /**
     * Whether the player may continue behind this range instead of playing it. Ranges that
     * only need to be muted or that are just markers are not skipped.
     */
    val isSkippedDuringPlayback: Boolean
        get() = type == TYPE_CUT || type == TYPE_COMMERCIAL

    companion object {
        const val TYPE_UNKNOWN = -1
        const val TYPE_CUT = 0
        const val TYPE_MUTE = 1
        const val TYPE_SCENE = 2
        const val TYPE_COMMERCIAL = 3
    }
}
