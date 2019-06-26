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
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.ParsableByteArray
import com.google.android.exoplayer2.video.AvcConfig

import org.tvheadend.htsp.HtspMessage

import timber.log.Timber

internal class H264StreamReader : PlainStreamReader(C.TRACK_TYPE_VIDEO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var initializationData: List<ByteArray>? = null

        if (stream.containsKey("meta")) {
            try {
                val avcConfig = AvcConfig.parse(ParsableByteArray(stream.getByteArray("meta")))
                initializationData = avcConfig.initializationData
            } catch (e: ParserException) {
                Timber.e("Failed to parse H264 meta, discarding")
            }
        }

        return Format.createVideoSampleFormat(
                Integer.toString(streamIndex),
                MimeTypes.VIDEO_H264, null,
                Format.NO_VALUE,
                Format.NO_VALUE,
                stream.getInteger("width"),
                stream.getInteger("height"),
                StreamReaderUtils.frameDurationToFrameRate(stream.getInteger("duration", Format.NO_VALUE)),
                initializationData, null)
    }

    override fun getTrackType(): Int {
        return C.TRACK_TYPE_VIDEO
    }
}
