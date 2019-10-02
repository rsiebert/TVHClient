/*
 * Copyright (c) 2017 Kiall Mac Innes <kiall@macinnes.ie>
 * Copyright (C) 2016 The Android Open Source Project
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
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.util.MimeTypes
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TvhMappings
import timber.log.Timber
import java.util.*

internal class VorbisStreamReader : PlainStreamReader(C.TRACK_TYPE_AUDIO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var initializationData: List<ByteArray>? =
                null

        if (stream.containsKey("meta")) {
            try {
                initializationData = parseVorbisCodecPrivate(stream.getByteArray("meta"))
            } catch (e: ParserException) {
                Timber.e("Failed to parse Vorbis meta, discarding")
            }

        }

        var rate = Format.NO_VALUE
        if (stream.containsKey("rate")) {
            rate = TvhMappings.sriToRate(stream.getInteger("rate"))
        }

        return Format.createAudioSampleFormat(
                streamIndex.toString(),
                MimeTypes.AUDIO_VORBIS, null,
                Format.NO_VALUE,
                Format.NO_VALUE,
                stream.getInteger("channels", Format.NO_VALUE),
                rate,
                C.ENCODING_PCM_16BIT,
                initializationData, null,
                C.SELECTION_FLAG_AUTOSELECT,
                stream.getString("language", "und")
        )
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_AUDIO

    /**
     * Builds initialization data for a [Format] from Vorbis codec private data.
     *
     * @return The initialization data for the [Format].
     * @throws ParserException If the initialization data could not be built.
     */
    @Throws(ParserException::class)
    private fun parseVorbisCodecPrivate(codecPrivate: ByteArray): List<ByteArray> {
        try {
            if (codecPrivate[0].toInt() != 0x02) {
                throw ParserException("Error parsing vorbis codec private")
            }
            var offset = 1
            var vorbisInfoLength = 0
            while (codecPrivate[offset] == 0xFF.toByte()) {
                vorbisInfoLength += 0xFF
                offset++
            }
            vorbisInfoLength += codecPrivate[offset++].toInt()

            var vorbisSkipLength = 0
            while (codecPrivate[offset] == 0xFF.toByte()) {
                vorbisSkipLength += 0xFF
                offset++
            }
            vorbisSkipLength += codecPrivate[offset++].toInt()

            if (codecPrivate[offset].toInt() != 0x01) {
                throw ParserException("Error parsing vorbis codec private")
            }
            val vorbisInfo = ByteArray(vorbisInfoLength)
            System.arraycopy(codecPrivate, offset, vorbisInfo, 0, vorbisInfoLength)
            offset += vorbisInfoLength
            if (codecPrivate[offset].toInt() != 0x03) {
                throw ParserException("Error parsing vorbis codec private")
            }
            offset += vorbisSkipLength
            if (codecPrivate[offset].toInt() != 0x05) {
                throw ParserException("Error parsing vorbis codec private")
            }
            val vorbisBooks = ByteArray(codecPrivate.size - offset)
            System.arraycopy(codecPrivate, offset, vorbisBooks, 0, codecPrivate.size - offset)
            val initializationData = ArrayList<ByteArray>(2)
            initializationData.add(vorbisInfo)
            initializationData.add(vorbisBooks)
            return initializationData
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ParserException("Error parsing vorbis codec private")
        }

    }

}
