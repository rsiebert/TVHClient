package org.tvheadend.tvhclient.debug

import android.content.Context
import timber.log.Timber

/**
 * Loads the demo recording that [FakeServerBootstrap] hands to the fake HTSP server, so the internal
 * player has something real to play.
 *
 * Drop a few seconds of video into `app/src/debug/assets/` named `demo_recording.<ext>`. The file is
 * served byte for byte, so the container decides what plays. The extractors the internal player uses
 * (`TvheadendExtractorsFactory`) sniff:
 *
 *  - **Matroska** (`.mkv`) and **fragmented MP4** (`.mp4`) — the best fit, because they carry a video
 *    *and* an audio track, which is what makes the track selection dialog show both tabs
 *  - MPEG-TS (`.ts`), FLV (`.flv`), Ogg (`.ogg`) and WAV (`.wav`) as well
 *
 * The clip only needs to be a couple of seconds and a few hundred kilobytes: the player seeks within
 * it, so it is also what exercises the ranged reads of the HTSP file API.
 *
 * The asset sits in the **debug** source set, so it is never part of a release build. When it is
 * absent [load] returns `null` and the fake server falls back to its generated silent WAV, which
 * keeps the demo working out of the box.
 */
object DemoRecordingAsset {

    /** Prefix of the bundled clip; any extension is accepted after it. */
    const val NAME_PREFIX = "demo_recording"

    /**
     * The name of the demo clip in [names], or `null` when none of them is one. Split out from
     * [load] so the naming rule can be tested without an Android context; the assets folder also
     * holds a README, which must not be mistaken for a clip.
     */
    fun assetName(names: List<String>): String? =
            names.firstOrNull { it.startsWith(NAME_PREFIX) }

    /**
     * The bundled clip, or `null` when none was added.
     *
     * Deliberately total: the clip is optional, so starting the demo must never depend on it. A
     * context or asset manager that cannot be read at all - a mock in a unit test, for one - has to
     * degrade to the generated WAV instead of failing [FakeServerBootstrap.start].
     */
    fun load(context: Context): ByteArray? = try {
        readClip(context)
    } catch (e: Exception) {
        Timber.w(e, "Could not read the demo recording asset, serving the generated WAV instead")
        null
    }

    private fun readClip(context: Context): ByteArray? {
        val assets = context.assets
        val name = assetName(assets.list("").orEmpty().toList()) ?: run {
            Timber.d("No $NAME_PREFIX.* asset, the fake server serves its generated WAV instead")
            return null
        }

        val bytes = assets.open(name).use { it.readBytes() }
        Timber.i("Serving the demo recording asset $name (${bytes.size} bytes)")
        return bytes
    }
}
