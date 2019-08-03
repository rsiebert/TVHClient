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

import com.google.android.exoplayer2.Format

internal class StreamReaderUtils private constructor() {
    init {
        throw IllegalAccessError("Utility class")
    }

    companion object {

        fun frameDurationToFrameRate(frameDuration: Int): Float {
            var frameRate = Format.NO_VALUE.toFloat()

            if (frameDuration != Format.NO_VALUE) {
                // 1000000 = 1 second, in microseconds.
                frameRate = 1000000 / frameDuration.toFloat()
            }

            return frameRate
        }
    }
}
