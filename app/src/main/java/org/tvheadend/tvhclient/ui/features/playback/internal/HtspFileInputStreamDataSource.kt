package org.tvheadend.tvhclient.ui.features.playback.internal

import android.net.Uri
import androidx.media3.common.C.RESULT_END_OF_INPUT
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.api.ServerMessageListener
import org.tvheadend.api.ServerResponseListener
import timber.log.Timber
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class HtspFileInputStreamDataSource private constructor(val connection: HtspConnection) : DataSource, Closeable, ServerMessageListener<HtspMessage>, HtspDataSourceInterface {

    private val dataSourceCount = AtomicInteger()

    private val htspConnection: HtspConnection = connection
    private lateinit var dataSpec: DataSpec
    private var dataSourceNumber = 0

    private lateinit var byteBuffer: ByteBuffer

    private var fileName: String? = null
    private var fileId = -1
    private var fileSize: Long = -1
    private var filePosition: Long = 0


    class Factory internal constructor(htspConnection: HtspConnection) : DataSource.Factory {

        private val htspConnection: HtspConnection
        private var dataSource: HtspFileInputStreamDataSource? = null

        override fun createDataSource(): DataSource {
            Timber.d("Created new data source from factory")
            val newDataSource = HtspFileInputStreamDataSource(htspConnection)
            dataSource = newDataSource
            return newDataSource
        }

        val currentDataSource: HtspFileInputStreamDataSource?
            get() {
                Timber.d("Returning data source")
                return if (dataSource != null) dataSource else null
            }

        fun releaseCurrentDataSource() {
            Timber.d("Releasing data source")
            dataSource?.release()
        }

        init {
            Timber.d("Initializing subscription data source factory")
            this.htspConnection = htspConnection
        }
    }

    init {
        Timber.d("Initializing file input data source")
        htspConnection.addMessageListener(this)
        dataSourceNumber = dataSourceCount.incrementAndGet()
    }

    override val timeshiftOffsetPts: Long
        get() = Long.MIN_VALUE

    override val timeshiftStartTime: Long
        get() = Long.MIN_VALUE

    override val timeshiftStartPts: Long
        get() = Long.MIN_VALUE

    override fun setSpeed(tvhSpeed: Int) {
        // NOP
    }

    override fun resume() {
        // No action needed
    }

    override fun pause() {
        // No action needed
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // NOP
    }

    override fun open(spec: DataSpec): Long {
        Timber.d("Opening file input data source $dataSourceNumber)")
        dataSpec = spec

        fileName = "dvrfile" + dataSpec.uri.path

        val fileReadRequest = HtspMessage()
        fileReadRequest["method"] = "fileRead"
        fileReadRequest["size"] = 1024000

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val fileReadHandler = object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    val error = response.getString("error")
                    Timber.d("Error reading file at offset 0: %s", error)
                } else {
                    val data = response.getByteArray("data")
                    Timber.d("Fetched %s bytes of file at filePosition %s", data.size, filePosition)
                    filePosition += data.size.toLong()
                    byteBuffer = ByteBuffer.wrap(data)
                }
                lock.withLock {
                    Timber.d("Notifying fileReadRequest")
                    condition.signal()
                }
            }
        }

        val fileOpenRequest = HtspMessage()
        fileOpenRequest["method"] = "fileOpen"
        fileOpenRequest["file"] = fileName

        htspConnection.sendMessage(fileOpenRequest, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    val error = response.getString("error")
                    Timber.d("Error opening file: %s", error)
                } else {
                    Timber.d("Opening file: %s", fileName)
                    fileId = response.getInteger("id")
                    if (response.containsKey("size")) {
                        fileSize = response.getLong("size")
                        Timber.v("Opened file $fileName of size $fileSize successfully")
                    } else {
                        Timber.v("Opened file $fileName successfully")
                    }
                    Timber.d("Sending file read request for file id %s", fileId)
                    fileReadRequest["id"] = fileId
                    htspConnection.sendMessage(fileReadRequest, fileReadHandler)
                }
            }
        })

        Timber.d("Waiting for fileReadRequest")
        lock.withLock {
            try {
                condition.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted")
            }
        }

        // The server provides the size of the recording in the reply to the open request.
        // If it did not, ask for it, otherwise the number of remaining bytes stays unknown.
        if (fileId >= 0 && fileSize < 0) {
            Timber.d("Size of the file is not known, requesting it from the server")
            updateFileSizeFromServer()
        }

        // The server opens the file at the beginning. When playback shall continue at a later
        // position, for example after the player has seeked, the file position is moved to it.
        val startPosition = if (spec.position > 0) spec.position else 0
        if (fileId >= 0 && startPosition > 0) {
            Timber.d("File shall be opened at position $startPosition")
            if (seekTo(startPosition)) {
                // The data of the first read request was read from the beginning of the file
                // and is therefore not the data that was asked for. It can be discarded.
                Timber.d("Discarding the data that was read from the beginning of the file")
                byteBuffer = ByteBuffer.allocate(0)
            }
        }

        Timber.d("Opened file $fileName, id $fileId with size $fileSize at position $filePosition")
        // The player can only read the data that is left after the requested position. This is
        // measured from the position that was asked for, not from [filePosition]: the speculative
        // read above already moved that cursor to the end of a file that fits in one chunk, which
        // made the open report zero remaining bytes and left the player unable to parse the
        // container at all.
        return if (fileSize != -1L) fileSize - startPosition else fileSize
    }

    override fun read(bytes: ByteArray, offset: Int, readLength: Int): Int {
        Timber.d("Read %s at offset %s with length %s", bytes.size, offset, readLength)
        // If we've reached the end of the file, we're done :)
        // If we've reached the end of the file, we're done :)
        if (fileSize == filePosition && !byteBuffer.hasRemaining()) {
            Timber.d("File has been read, returning -1")
            return RESULT_END_OF_INPUT
        }

        sendFileRead(filePosition)

        if (!byteBuffer.hasRemaining() && fileSize == -1L) {
            Timber.d("No data and no known size, returning -1")
            // If we still don't have any data, and we
            // don't have a known size, then we're done.
            return RESULT_END_OF_INPUT
        } else if (!byteBuffer.hasRemaining()) {
            // If we don't have data here, something went wrong
            Timber.d("Failed to read data for %s, returning -1", fileName)
            return RESULT_END_OF_INPUT
        }

        val length = copyBufferedBytes(byteBuffer, bytes, offset, readLength)
        Timber.d("Read %s bytes into the buffer at offset %s", length, offset)
        return length
    }

    override fun getUri(): Uri? {
        Timber.d("Returning data spec uri of %s", dataSpec.uri)
        return dataSpec.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        Timber.d("Returning response headers")
        return emptyMap()
    }

    override fun close() {
        Timber.d("Closing file input data source $dataSourceNumber)")
    }

    override fun onMessage(response: HtspMessage, method: String) {
        // NOP
    }

    // HtspDataSource Methods
    private fun release() {
        Timber.d("Releasing file input data source $dataSourceNumber)")

        val request = HtspMessage()
        request["method"] = "fileClose"
        request["id"] = fileId

        // Tell the server where the playback stopped so that it can store the position and
        // any other client can continue the playback at it. The value is expected in seconds.
        val playPosition = getPlayPositionSeconds()
        if (fileId >= 0 && playPosition > 0) {
            Timber.d("Reporting a playback position of $playPosition seconds to the server")
            request["playposition"] = playPosition
        }

        htspConnection.sendMessage(request, null)
        htspConnection.removeMessageListener(this)
    }

    /**
     * Returns the position in the file that the player has actually consumed, in seconds.
     * The data that was fetched from the server but not yet read by the player is subtracted.
     */
    private fun getPlayPositionSeconds(): Long {
        if (filePosition <= 0) {
            return 0
        }
        val buffered = if (this::byteBuffer.isInitialized) byteBuffer.remaining().toLong() else 0
        return ((filePosition - buffered).coerceAtLeast(0)) / 1000
    }

    /**
     * Moves the file position of the file handle that the server keeps for this data source.
     * Returns whether the server accepted the new position.
     */
    private fun seekTo(position: Long): Boolean {
        Timber.d("Seeking file $fileId to position $position")

        val request = HtspMessage()
        request["method"] = "fileSeek"
        request["id"] = fileId
        request["offset"] = position
        request["whence"] = "SEEK_SET"

        val response = sendFileRequest(request) ?: return false
        if (response.containsKey("error")) {
            Timber.d("Error seeking in the file: %s", response.getString("error"))
            return false
        }

        filePosition = response.getLong("offset", position)
        Timber.d("File position is now $filePosition")
        return true
    }

    /**
     * Asks the server for the size of the file. This is used when the reply to the open
     * request did not contain the size of the file.
     */
    private fun updateFileSizeFromServer() {
        val request = HtspMessage()
        request["method"] = "fileStat"
        request["id"] = fileId

        val response = sendFileRequest(request) ?: return
        if (response.containsKey("error")) {
            Timber.d("Error requesting the size of the file: %s", response.getString("error"))
            return
        }

        if (response.containsKey("size")) {
            fileSize = response.getLong("size")
            Timber.d("Size of the file is $fileSize")
        }
    }

    /**
     * Sends the given file request and waits until the server has replied. Returns null when
     * the server did not reply in time. This method must not be called from a message handler,
     * because those are invoked on the connection thread that also delivers the reply.
     */
    private fun sendFileRequest(request: HtspMessage, timeoutMs: Long = 5_000): HtspMessage? {
        val lock = ReentrantLock()
        val condition = lock.newCondition()
        var reply: HtspMessage? = null

        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                reply = response
                lock.withLock {
                    condition.signal()
                }
            }
        })

        lock.withLock {
            try {
                condition.await(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Timber.d(e, "Waiting for a reply from the server was interrupted")
            }
        }
        return reply
    }

    private fun sendFileRead(offset: Long) {
        Timber.d("Sending message to read file from offset %s", offset)

        if (byteBuffer.hasRemaining()) {
            Timber.d("Buffer has elements remaining, returning")
            return
        }

        var size: Long = 1024000
        Timber.d("File size is %s", fileSize)
        if (fileSize != -1L) {
            // Make sure we don't overrun the file
            if (offset + size > fileSize) {
                size = fileSize - offset
            }
        }

        val request = HtspMessage()
        request["method"] = "fileRead"
        request["id"] = fileId
        request["size"] = size
        request["offset"] = offset
        Timber.d("Fetching $size bytes of file at offset $offset")

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    val error = response.getString("error")
                    Timber.d("Error reading file at $offset: $error")
                } else {
                    val data = response.getByteArray("data")
                    Timber.d("Fetched %s bytes of file at offset %s", data.size, offset)
                    filePosition += data.size.toLong()
                    byteBuffer = ByteBuffer.wrap(data)
                }
                lock.withLock {
                    condition.signal()
                }
            }
        })
        Timber.d("Waiting for file read request")
        lock.withLock {
            try {
                condition.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted.")
            }
        }
    }
}

/**
 * Copies up to [readLength] bytes from [buffer] into [bytes], starting at [offset], and returns
 * the number of bytes that were copied. The buffer is drained by exactly that number of bytes,
 * which is what the caller has to report to the player.
 */
internal fun copyBufferedBytes(buffer: ByteBuffer, bytes: ByteArray, offset: Int, readLength: Int): Int {
    if (readLength <= 0 || !buffer.hasRemaining()) {
        return 0
    }
    val length = min(readLength, buffer.remaining())
    buffer[bytes, offset, length]
    return length
}
