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

package org.tvheadend.tvhclient.ui.features.playback.internal.reader

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.util.MimeTypes

import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TvhMappings

internal class Mpeg2AudioStreamReader : PlainStreamReader(C.TRACK_TYPE_AUDIO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var rate = Format.NO_VALUE
        if (stream.containsKey("rate")) {
            rate = TvhMappings.sriToRate(stream.getInteger("rate"))
        }

        // TVHeadend calls all MPEG Audio MPEG2AUDIO - e.g. it could be either mp2 or mp3 audio. We
        // need to use the new audio_version field (4.1.2498+ only). Default to mp2 as that's most
        // common for DVB.
        var audioVersion = 2

        if (stream.containsKey("audio_version")) {
            audioVersion = stream.getInteger("audio_version")
        }

        val mimeType: String = when (audioVersion) {
            1 -> MimeTypes.AUDIO_MPEG_L1    // MP1 Audio - V.Unlikely these days
            2 -> MimeTypes.AUDIO_MPEG_L2    // MP2 Audio - Pretty common in DVB streams
            3 -> MimeTypes.AUDIO_MPEG       // MP3 Audio - Pretty common in IPTV streams
            else -> throw RuntimeException("Unknown MPEG Audio Version: $audioVersion")
        }

        return Format.createAudioSampleFormat(
                streamIndex.toString(),
                mimeType,
                null,
                Format.NO_VALUE,
                Format.NO_VALUE,
                stream.getInteger("channels", Format.NO_VALUE),
                rate,
                C.ENCODING_PCM_16BIT, null, null,
                C.SELECTION_FLAG_AUTOSELECT,
                stream.getString("language", "und")
        )
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_AUDIO
}
