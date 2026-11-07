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

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.ParserException
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.HevcConfig

import org.tvheadend.htsp.HtspMessage

import timber.log.Timber

internal class H265StreamReader : PlainStreamReader(C.TRACK_TYPE_VIDEO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        var initializationData: List<ByteArray>? =
                null

        if (stream.containsKey("meta")) {
            try {
                val hevcConfig = HevcConfig.parse(ParsableByteArray(stream.getByteArray("meta")))
                initializationData = hevcConfig.initializationData
            } catch (e: ParserException) {
                Timber.e("Failed to parse H265 meta, discarding")
            }

        }

        return Format.Builder()
                .setId(streamIndex.toString())
                .setSampleMimeType(MimeTypes.VIDEO_H265)
                .setAverageBitrate(Format.NO_VALUE)
                .setMaxInputSize(Format.NO_VALUE)
                .setWidth(stream.getInteger("width"))
                .setHeight(stream.getInteger("height"))
                .setFrameRate(StreamReaderUtils.frameDurationToFrameRate(stream.getInteger("duration", Format.NO_VALUE)))
                .setInitializationData(initializationData)
                .build()
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_VIDEO
}
