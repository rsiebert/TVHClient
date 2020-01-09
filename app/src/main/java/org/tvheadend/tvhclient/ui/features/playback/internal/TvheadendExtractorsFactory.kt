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

import com.google.android.exoplayer2.ext.flac.FlacExtractor
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.flv.FlvExtractor
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.extractor.ogg.OggExtractor
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor
import com.google.android.exoplayer2.extractor.ts.PsExtractor
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.extractor.wav.WavExtractor

internal class TvheadendExtractorsFactory : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        return arrayOf(
                HtspSubscriptionExtractor(),
                MatroskaExtractor(MatroskaExtractor.RESULT_CONTINUE),
                FragmentedMp4Extractor(FragmentedMp4Extractor.RESULT_CONTINUE),
                Mp4Extractor(),
                Mp3Extractor(Mp3Extractor.RESULT_CONTINUE),
                AdtsExtractor(),
                Ac3Extractor(),
                TsExtractor(TsExtractor.MODE_MULTI_PMT),
                FlvExtractor(),
                OggExtractor(),
                PsExtractor(),
                FlacExtractor(),
                WavExtractor())
    }
}
