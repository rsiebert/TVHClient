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
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
import com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import org.tvheadend.tvhclient.R
import timber.log.Timber
import java.util.*

internal class TvheadendRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    override fun buildVideoRenderers(context: Context,
                                     extensionRendererMode: Int,
                                     mediaCodecSelector: MediaCodecSelector,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     playClearSamplesWithoutKeys: Boolean,
                                     enableDecoderFallback: Boolean,
                                     eventHandler: Handler,
                                     eventListener: VideoRendererEventListener,
                                     allowedVideoJoiningTimeMs: Long,
                                     out: ArrayList<Renderer>) {

        Timber.d("Adding MediaCodecVideoRenderer")
        out.add(MediaCodecVideoRenderer(
                context,
                MediaCodecSelector.DEFAULT,
                allowedVideoJoiningTimeMs,
                false,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY))
    }

    override fun buildAudioRenderers(context: Context,
                                     extensionRendererMode: Int,
                                     mediaCodecSelector: MediaCodecSelector,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     playClearSamplesWithoutKeys: Boolean,
                                     enableDecoderFallback: Boolean,
                                     audioProcessors: Array<out AudioProcessor>,
                                     eventHandler: Handler,
                                     eventListener: AudioRendererEventListener,
                                     out: ArrayList<Renderer>) {

        val audioCapabilities = AudioCapabilities.getCapabilities(context)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val enablePassthroughDecoder = sharedPreferences.getBoolean(
                "audio_passthrough_decoder_enabled",
                context.resources.getBoolean(R.bool.pref_default_audio_passthrough_decoder_enabled))

        // Native Audio Decoders
        Timber.d("Adding MediaCodecAudioRenderer")
        val customMediaCodecSelector = buildMediaCodecSelector(enablePassthroughDecoder)
        out.add(MediaCodecAudioRenderer(context, customMediaCodecSelector,
                true, eventHandler, eventListener, DefaultAudioSink(audioCapabilities, audioProcessors)))

        // FFMpeg Audio Decoder
        Timber.d("Adding FfmpegAudioRenderer")
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, *audioProcessors))

        // Flac Audio Decoder
        Timber.d("Adding FlacAudioRenderer")
        out.add(LibflacAudioRenderer(eventHandler, eventListener, *audioProcessors))
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
            override fun getDecoderInfos(mimeType: String, requiresSecureDecoder: Boolean, requiresTunnelingDecoder: Boolean): MutableList<MediaCodecInfo> {
                return MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            }

            override fun getPassthroughDecoderInfo(): MediaCodecInfo? {
                return if (enablePassthroughDecoder) {
                    MediaCodecUtil.getPassthroughDecoderInfo()
                } else {
                    null
                }
            }
        }
    }
}
