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
import androidx.media3.common.C.SELECTION_FLAG_DEFAULT
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.tvheadend.htsp.HtspMessage

internal class DvbsubStreamReader : PlainStreamReader(C.TRACK_TYPE_TEXT) {

    override fun buildFormat(streamIndex: Int, stream: HtspMessage): Format {
        val compositionId = stream.getInteger("composition_id")
        val ancillaryId = stream.getInteger("ancillary_id")
        val initializationData = listOf(byteArrayOf((compositionId shr 8 and 0xFF).toByte(), (compositionId and 0xFF).toByte(), (ancillaryId shr 8 and 0xFF).toByte(), (ancillaryId and 0xFF).toByte()))

        return Format.Builder()
                .setId(streamIndex.toString())
                .setSampleMimeType(MimeTypes.APPLICATION_DVBSUBS)
                .setAverageBitrate(Format.NO_VALUE)
                .setSelectionFlags(SELECTION_FLAG_DEFAULT)
                .setInitializationData(initializationData)
                .setLanguage(stream.getString("language", "und"))
                .build()
    }

    override val trackType: Int
        get() = C.TRACK_TYPE_TEXT
}
