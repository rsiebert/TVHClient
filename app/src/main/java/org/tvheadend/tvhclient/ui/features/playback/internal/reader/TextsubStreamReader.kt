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
import com.google.android.exoplayer2.extractor.ExtractorOutput
import com.google.android.exoplayer2.extractor.TrackOutput
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.ParsableByteArray
import com.google.android.exoplayer2.util.Util
import org.tvheadend.htsp.HtspMessage
import java.nio.charset.Charset
import java.util.*

internal class TextsubStreamReader : StreamReader {

    private var mTrackOutput: TrackOutput? = null

    override fun createTracks(stream: HtspMessage, output: ExtractorOutput) {
        val streamIndex = stream.getInteger("index")
        mTrackOutput = output.track(streamIndex, C.TRACK_TYPE_TEXT)
        mTrackOutput!!.format(buildFormat(streamIndex, stream))
    }

    override fun consume(message: HtspMessage) {

        val pts = message.getLong("pts")
        val duration = message.getInteger("duration").toLong()
        val payload = Util.getUtf8Bytes(
                String(message.getByteArray("payload"), UTF_8).trim { it <= ' ' })

        val lengthWithPrefix = SUBRIP_PREFIX.size + payload.size
        val subsipSample = SUBRIP_PREFIX.copyOf(lengthWithPrefix)

        System.arraycopy(payload, 0, subsipSample, SUBRIP_PREFIX.size, payload.size)

        setSubripSampleEndTimecode(subsipSample, duration)

        mTrackOutput!!.sampleData(ParsableByteArray(subsipSample), lengthWithPrefix)
        mTrackOutput!!.sampleMetadata(pts, C.BUFFER_FLAG_KEY_FRAME, lengthWithPrefix, 0,
                null)
    }

    private fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        return Format.createTextSampleFormat(
                streamIndex.toString(),
                MimeTypes.APPLICATION_SUBRIP,
                C.SELECTION_FLAG_AUTOSELECT,
                stream.getString("language", "und"), null
        )
    }

    companion object {

        /**
         * A template for the prefix that must be added to each subrip sample. The 12 byte end timecode
         * starting at [.SUBRIP_PREFIX_END_TIMECODE_OFFSET] is set to a dummy value, and must be
         * replaced with the duration of the subtitle.
         *
         *
         * Equivalent to the UTF-8 string: "1\n00:00:00,000 --> 00:00:00,000\n".
         */
        private val SUBRIP_PREFIX = byteArrayOf(49, 10, 48, 48, 58, 48, 48, 58, 48, 48, 44, 48, 48, 48, 32, 45, 45, 62, 32, 48, 48, 58, 48, 48, 58, 48, 48, 44, 48, 48, 48, 10)
        /**
         * A special end timecode indicating that a subtitle should be displayed until the next subtitle,
         * or until the end of the media in the case of the last subtitle.
         *
         *
         * Equivalent to the UTF-8 string: "            ".
         */
        private val SUBRIP_TIMECODE_EMPTY = byteArrayOf(32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32)
        /**
         * The byte offset of the end timecode in [.SUBRIP_PREFIX].
         */
        private const val SUBRIP_PREFIX_END_TIMECODE_OFFSET = 19
        /**
         * The length in bytes of a timecode in a subrip prefix.
         */
        private const val SUBRIP_TIMECODE_LENGTH = 12

        // UTF-8 is the default on Android
        private val UTF_8 = Charset.defaultCharset()

        private fun setSubripSampleEndTimecode(subripSample: ByteArray, timeUs: Long) {
            var time = timeUs
            val timeCodeData: ByteArray
            if (time == C.TIME_UNSET || time == 0L) {
                timeCodeData = SUBRIP_TIMECODE_EMPTY
            } else {
                val hours = (time / 3600000000L).toInt()
                time -= hours * 3600000000L
                val minutes = (time / 60000000).toInt()
                time -= (minutes * 60000000).toLong()
                val seconds = (time / 1000000).toInt()
                time -= (seconds * 1000000).toLong()
                val milliseconds = (time / 1000).toInt()
                timeCodeData = Util.getUtf8Bytes(String.format(Locale.US, "%02d:%02d:%02d,%03d", hours,
                        minutes, seconds, milliseconds))
            }

            System.arraycopy(timeCodeData, 0, subripSample, SUBRIP_PREFIX_END_TIMECODE_OFFSET,
                    SUBRIP_TIMECODE_LENGTH)
        }
    }
}
