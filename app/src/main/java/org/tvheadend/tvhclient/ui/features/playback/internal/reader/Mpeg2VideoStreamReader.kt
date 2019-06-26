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

internal class Mpeg2VideoStreamReader : PlainStreamReader(C.TRACK_TYPE_VIDEO) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        return Format.createVideoSampleFormat(
                Integer.toString(streamIndex),
                MimeTypes.VIDEO_MPEG2,
                null,
                Format.NO_VALUE,
                Format.NO_VALUE,
                stream.getInteger("width"),
                stream.getInteger("height"),
                StreamReaderUtils.frameDurationToFrameRate(stream.getInteger("duration", Format.NO_VALUE)), null, null)
    }

    override fun getTrackType(): Int {
        return C.TRACK_TYPE_VIDEO
    }
}
