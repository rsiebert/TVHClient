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

import org.tvheadend.htsp.HtspMessage

internal class Mpeg2VideoStreamReader : PlainStreamReader(C.TRACK_TYPE_VIDEO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        return Format.Builder()
                .setId(streamIndex.toString())
                .setSampleMimeType(MimeTypes.VIDEO_MPEG2)
                .setAverageBitrate(Format.NO_VALUE)
                .setMaxInputSize(Format.NO_VALUE)
                .setWidth(stream.getInteger("width"))
                .setHeight(stream.getInteger("height"))
                .setFrameRate(StreamReaderUtils.frameDurationToFrameRate(stream.getInteger("duration", Format.NO_VALUE)))
                .build()
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_VIDEO
}
