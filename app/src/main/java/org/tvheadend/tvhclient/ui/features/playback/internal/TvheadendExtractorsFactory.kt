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

package org.tvheadend.tvhclient.ui.features.playback.internal

import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.flv.FlvExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.ts.PsExtractor
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.wav.WavExtractor

internal class TvheadendExtractorsFactory : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        return arrayOf(
                HtspSubscriptionExtractor(),
                MatroskaExtractor(),
                FragmentedMp4Extractor(),
                Mp4Extractor(),
                Mp3Extractor(),
                AdtsExtractor(),
                Ac3Extractor(),
                TsExtractor(),
                FlvExtractor(),
                OggExtractor(),
                PsExtractor(),
                WavExtractor())
    }
}
