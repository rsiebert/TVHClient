/*
 * Copyright (c) 2017 Kiall Mac Innes <kiall@macinnes.ie>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tvheadend.tvhclient.ui.features.playback.internal

import android.content.Context
import android.os.Handler
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import org.tvheadend.tvhclient.R
import timber.log.Timber
import java.util.*

internal class TvheadendRenderersFactory(context: Context) : DefaultRenderersFactory(context, null, EXTENSION_RENDERER_MODE_PREFER, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) {

    /**
     * Builds video renderers for use by the player.
     *
     * @param context                   The [Context] associated with the player.
     * @param drmSessionManager         An optional [DrmSessionManager]. May be null if the player
     * will not be used for DRM protected playbacks.
     * @param allowedVideoJoiningTimeMs The maximum duration in milliseconds for which video
     * renderers can attempt to seamlessly join an ongoing playback.
     * @param eventHandler              A handler associated with the main thread's looper.
     * @param eventListener             An event listener.
     * @param extensionRendererMode     The extension renderer mode.
     * @param out                       An array to which the built renderers should be appended.
     */
    override fun buildVideoRenderers(context: Context,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     allowedVideoJoiningTimeMs: Long,
                                     eventHandler: Handler,
                                     eventListener: VideoRendererEventListener,
                                     @ExtensionRendererMode extensionRendererMode: Int,
                                     out: ArrayList<Renderer>) {

        Timber.d("Adding MediaCodecVideoRenderer")
        out.add(MediaCodecVideoRenderer(
                context,
                MediaCodecSelector.DEFAULT,
                allowedVideoJoiningTimeMs,
                drmSessionManager,
                false,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY))
    }

    /**
     * Builds audio renderers for use by the player.
     *
     * @param context               The [Context] associated with the player.
     * @param drmSessionManager     An optional [DrmSessionManager]. May be null if the player
     * will not be used for DRM protected playbacks.
     * @param audioProcessors       An array of [AudioProcessor]s that will process PCM audio
     * buffers before output. May be empty.
     * @param eventHandler          A handler to use when invoking event listeners and outputs.
     * @param eventListener         An event listener.
     * @param extensionRendererMode The extension renderer mode.
     * @param out                   An array to which the built renderers should be appended.
     */
    override fun buildAudioRenderers(context: Context,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     audioProcessors: Array<AudioProcessor>,
                                     eventHandler: Handler,
                                     eventListener: AudioRendererEventListener,
                                     @ExtensionRendererMode extensionRendererMode: Int,
                                     out: ArrayList<Renderer>) {

        val audioCapabilities = AudioCapabilities.getCapabilities(context)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val enablePassthroughDecoder = sharedPreferences.getBoolean(
                "audio_passthrough_decoder_enabled",
                context.resources.getBoolean(R.bool.pref_default_audio_passthrough_decoder_enabled))

        // Native Audio Decoders
        Timber.d("Adding MediaCodecAudioRenderer")
        val mediaCodecSelector = buildMediaCodecSelector(enablePassthroughDecoder)
        out.add(MediaCodecAudioRenderer(mediaCodecSelector, drmSessionManager,
                true, eventHandler, eventListener, audioCapabilities))

        // FFMpeg Audio Decoder
        Timber.d("Adding FfmpegAudioRenderer")
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, *audioProcessors))
    }

    /**
     * Builds a MediaCodecSelector that can explicitly disable audio passthrough
     *
     * @param enablePassthroughDecoder True if audio passthrough shall be enabled, disabled otherwise
     * @return The MediaCodecSelector
     */
    private fun buildMediaCodecSelector(enablePassthroughDecoder: Boolean): MediaCodecSelector {
        return object : MediaCodecSelector {
            @Throws(MediaCodecUtil.DecoderQueryException::class)
            override fun getDecoderInfo(mimeType: String, requiresSecureDecoder: Boolean): MediaCodecInfo? {
                return MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder)
            }

            override fun getPassthroughDecoderInfo(): MediaCodecInfo? {
                return if (enablePassthroughDecoder) {
                    MediaCodecUtil.getPassthroughDecoderInfo()
                } else null
            }
        }
    }
}
