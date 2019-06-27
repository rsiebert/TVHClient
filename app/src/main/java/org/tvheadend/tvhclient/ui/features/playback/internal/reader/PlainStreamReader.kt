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
import com.google.android.exoplayer2.util.ParsableByteArray
import leakcanary.LeakSentry
import org.tvheadend.htsp.HtspMessage

/**
 * A PlainStreamReader simply copies the raw bytes from muxpkt's over onto the track output
 */
abstract class PlainStreamReader(private val mTrackType: Int) : StreamReader {
    private var mTrackOutput: TrackOutput? = null

    protected abstract val trackType: Int

    override fun createTracks(stream: HtspMessage, output: ExtractorOutput) {
        val streamIndex = stream.getInteger("index")
        mTrackOutput = output.track(streamIndex, trackType)
        mTrackOutput!!.format(buildFormat(streamIndex, stream))
    }

    override fun consume(message: HtspMessage) {
        val pts = message.getLong("pts")
        val frameType = message.getInteger("frametype", -1)
        val payload = message.getByteArray("payload")
        val pba = ParsableByteArray(payload)

        var bufferFlags = 0

        if (mTrackType == C.TRACK_TYPE_VIDEO) {
            // We're looking at a Video stream, be picky about what frames are called keyframes

            // Type -1 = TVHeadend has not provided us a frame type, so everything "is a keyframe"
            // Type 73 = I - Intra-coded picture - Full Picture
            // Type 66 = B - Predicted picture - Depends on previous frames
            // Type 80 = P - Bidirectional predicted picture - Depends on previous+future frames
            if (frameType == -1 || frameType == 73) {
                bufferFlags = bufferFlags or C.BUFFER_FLAG_KEY_FRAME
            }
        } else {
            // We're looking at a Audio / Text etc stream, consider everything a key frame
            bufferFlags = bufferFlags or C.BUFFER_FLAG_KEY_FRAME
        }

        mTrackOutput!!.sampleData(pba, payload.size)
        mTrackOutput!!.sampleMetadata(pts, bufferFlags, payload.size, 0, null)
    }

    override fun release() {
        // Watch for memory leaks
        LeakSentry.refWatcher.watch(this)
    }

    protected abstract fun buildFormat(streamIndex: Int, stream: HtspMessage): Format
}
