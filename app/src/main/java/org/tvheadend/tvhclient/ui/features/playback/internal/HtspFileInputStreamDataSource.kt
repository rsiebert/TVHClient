package org.tvheadend.tvhclient.ui.features.playback.internal

import android.net.Uri
import com.google.android.exoplayer2.C.RESULT_END_OF_INPUT
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
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

        override fun createDataSource(): DataSource? {
            Timber.d("Created new data source from factory")
            dataSource = HtspFileInputStreamDataSource(htspConnection)
            return dataSource
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

    override fun addTransferListener(transferListener: TransferListener?) {
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

        Timber.d("Opened file $fileName, id $fileId with size $fileSize")
        return fileSize
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

        Timber.d("Getting bytes %s from offset %s, read length: %s, buffer elements remaining: %s", bytes.size, offset, readLength, byteBuffer.remaining())
        byteBuffer[bytes, offset, min(readLength, byteBuffer.remaining())]
        val bytesRead = byteBuffer.position() - offset
        Timber.d("Read %s bytes", bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        Timber.d("Returning data spec uri of %s", dataSpec.uri)
        return dataSpec.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>>? {
        Timber.d("Returning response headers")
        return emptyMap()
    }

    override fun close() {
        Timber.d("Closing file input data source $dataSourceNumber)")
    }

    override fun onMessage(response: HtspMessage) {
        // NOP
    }

    // HtspDataSource Methods
    private fun release() {
        Timber.d("Releasing file input data source $dataSourceNumber)")
        val request = HtspMessage()
        request["method"] = "fileClose"
        request["id"] = fileId
        htspConnection.sendMessage(request, null)
        htspConnection.removeMessageListener(this)
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
                synchronized(request) {
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
