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

import android.util.SparseArray
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.extractor.*
import com.google.android.exoplayer2.util.ParsableByteArray
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.ui.features.playback.internal.reader.StreamReader
import org.tvheadend.tvhclient.ui.features.playback.internal.reader.StreamReadersFactory
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.util.*

internal class HtspSubscriptionExtractor : Extractor {

    private lateinit var mOutput: ExtractorOutput
    private val mStreamReaders = SparseArray<StreamReader>()
    private val mRawBytes = ByteArray(1024 * 1024)

    private class HtspSeekMap : SeekMap {
        override fun isSeekable(): Boolean {
            return true
        }

        override fun getDurationUs(): Long {
            return C.TIME_UNSET
        }

        override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints? {
            return null
        }
    }

    // Extractor Methods
    @Throws(IOException::class, InterruptedException::class)
    override fun sniff(input: ExtractorInput): Boolean {
        val scratch = ParsableByteArray(HtspSubscriptionDataSource.HEADER.size)
        // Find 8 bytes equal to HEADER at the start of the input.
        input.peekFully(scratch.data, 0, HtspSubscriptionDataSource.HEADER.size)
        return Arrays.equals(scratch.data, HtspSubscriptionDataSource.HEADER)
    }

    override fun init(output: ExtractorOutput) {
        Timber.i("Initializing HTSP Extractor")
        mOutput = output
        mOutput.seekMap(HtspSeekMap())
    }

    @Throws(IOException::class, InterruptedException::class)
    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        val bytesRead = input.read(mRawBytes, 0, mRawBytes.size)
        Timber.d("Read $bytesRead bytes")

        var objectInput: ObjectInputStream? = null
        try {
            ByteArrayInputStream(mRawBytes, 0, bytesRead).use { inputStream ->
                while (inputStream.available() > 0) {
                    objectInput = ObjectInputStream(inputStream)
                    handleMessage(objectInput!!.readUnshared() as HtspMessage)
                }
            }
        } catch (e: IOException) {
            // TODO: This is a problem, and returning RESULT_CONTINUE is a hack... I think?
            Timber.w("Caught IOException, returning RESULT_CONTINUE")
            return Extractor.RESULT_CONTINUE
        } catch (e: ClassNotFoundException) {
            Timber.w("Class Not Found")
        } finally {
            try {
                if (objectInput != null) {
                    objectInput!!.close()
                }
            } catch (ex: IOException) {
                // Ignore
            }

        }// N.B. Don't add the objectInput to this bit, it breaks stuff
        return Extractor.RESULT_CONTINUE
    }

    override fun seek(position: Long, timeUs: Long) {
        Timber.d("Seeking HTSP Extractor to position:$position and timeUs:$timeUs")
    }

    override fun release() {
        Timber.i("Releasing HTSP Extractor")
        mStreamReaders.clear()
    }

    // Internal Methods
    private fun handleMessage(message: HtspMessage) {
        val method = message.getString("method")

        if (method == "subscriptionStart") {
            handleSubscriptionStart(message)
        } else if (method == "muxpkt") {
            handleMuxpkt(message)
        }
    }

    private fun handleSubscriptionStart(message: HtspMessage) {
        Timber.d("Handling Subscription Start")

        val streamReadersFactory = StreamReadersFactory()

        for (obj in message.getList("streams")) {
            val stream = obj as HtspMessage
            val streamIndex = stream.getInteger("index")
            val streamType = stream.getString("type")
            val streamReader = streamReadersFactory.createStreamReader(streamType!!)
            if (streamReader != null) {
                Timber.d("Creating StreamReader for $streamType stream at index $streamIndex")
                streamReader.createTracks(stream, mOutput)
                mStreamReaders.put(streamIndex, streamReader)
            } else {
                Timber.d("Discarding stream at index $streamIndex, no suitable StreamReader")
            }
        }

        Timber.d("All streams have now been handled")
        mOutput.endTracks()
    }

    private fun handleMuxpkt(message: HtspMessage) {
        //        subscriptionId     u32   required   Subscription ID.
        //        frametype          u32   required   Type of frame as ASCII value: 'I', 'P', 'B'
        //        stream             u32   required   Stream index. Corresponds to the streams reported in the subscriptionStart message.
        //        dts                s64   optional   Decode Time Stamp in µs.
        //        pts                s64   optional   Presentation Time Stamp in µs.
        //        duration           u32   required   Duration of frame in µs.
        //        payload            bin   required   Actual frame data.

        // If the stream reader list contains null, then its not a stream we care about, so move on.
        val streamReader = mStreamReaders.get(message.getInteger("stream")) ?: return
        streamReader.consume(message)
    }
}
