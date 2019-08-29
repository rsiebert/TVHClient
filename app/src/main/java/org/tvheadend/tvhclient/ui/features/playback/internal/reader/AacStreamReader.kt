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
import com.google.android.exoplayer2.extractor.ExtractorOutput
import com.google.android.exoplayer2.extractor.TrackOutput
import com.google.android.exoplayer2.util.CodecSpecificDataUtil
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.ParsableByteArray
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TvhMappings

// See https://wiki.multimedia.cx/index.php?title=ADTS

internal class AacStreamReader : StreamReader {

    private var mTrackOutput: TrackOutput? = null

    override fun createTracks(stream: HtspMessage, output: ExtractorOutput) {
        val streamIndex = stream.getInteger("index")
        mTrackOutput = output.track(streamIndex, C.TRACK_TYPE_AUDIO)
        mTrackOutput!!.format(buildFormat(streamIndex, stream))
    }

    override fun consume(message: HtspMessage) {
        val pts = message.getLong("pts")
        val payload = message.getByteArray("payload")
        val pba = ParsableByteArray(payload)
        val skipLength: Int

        skipLength = if (hasCrc(payload[1])) {
            // Have a CRC
            ADTS_HEADER_SIZE + ADTS_CRC_SIZE
        } else {
            // No CRC
            ADTS_HEADER_SIZE
        }

        pba.skipBytes(skipLength)

        val aacFrameLength = payload.size - skipLength

        // TODO: Set Buffer Flag key frame based on frametype
        // frametype   u32   required   Type of frame as ASCII value: 'I', 'P', 'B'
        mTrackOutput!!.sampleData(pba, aacFrameLength)
        mTrackOutput!!.sampleMetadata(pts, C.BUFFER_FLAG_KEY_FRAME, aacFrameLength, 0, null)
    }

    override fun release() {
        // Nothing to be released
    }

    private fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        val initializationData: List<ByteArray>

        var rate = Format.NO_VALUE
        if (stream.containsKey("rate")) {
            rate = TvhMappings.sriToRate(stream.getInteger("rate"))
        }

        val channels = stream.getInteger("channels", Format.NO_VALUE)

        initializationData = if (stream.containsKey("meta")) {
            listOf(stream.getByteArray("meta"))
        } else {
            listOf(CodecSpecificDataUtil.buildAacLcAudioSpecificConfig(rate, channels))
        }

        return Format.createAudioSampleFormat(
                streamIndex.toString(),
                MimeTypes.AUDIO_AAC, null,
                Format.NO_VALUE,
                Format.NO_VALUE,
                channels,
                rate,
                C.ENCODING_PCM_16BIT,
                initializationData, null,
                C.SELECTION_FLAG_AUTOSELECT,
                stream.getString("language", "und")
        )
    }

    private fun hasCrc(b: Byte): Boolean {
        val data = b.toInt() and 0xFF
        return (data and 0x1) == 0
    }

    companion object {

        private const val ADTS_HEADER_SIZE = 7
        private const val ADTS_CRC_SIZE = 2
    }
}
