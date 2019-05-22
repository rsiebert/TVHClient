package org.tvheadend.tvhclient.ui.features.playback.internal;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.jetbrains.annotations.NotNull;
import org.tvheadend.htsp.HtspConnection;
import org.tvheadend.htsp.HtspMessage;
import org.tvheadend.htsp.HtspMessageListener;
import org.tvheadend.htsp.HtspResponseListener;
import org.tvheadend.tvhclient.MainApplication;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class HtspFileInputStreamDataSource implements DataSource, Closeable, HtspMessageListener, HtspDataSourceInterface {

    private static final AtomicInteger dataSourceCount = new AtomicInteger();
    private static final AtomicInteger subscriptionCount = new AtomicInteger();

    private final Context context;
    private final HtspConnection htspConnection;
    private DataSpec dataSpec;
    private final int dataSourceNumber;

    private ByteBuffer byteBuffer;

    private String fileName;
    private int fileId = -1;
    private long fileSize = -1;
    private long filePosition = 0;

    public static class Factory implements DataSource.Factory {

        private final Context context;
        private final HtspConnection htspConnection;
        private HtspFileInputStreamDataSource dataSource;

        Factory(Context context, HtspConnection htspConnection) {
            Timber.d("Initializing subscription data source factory");
            this.context = context;
            this.htspConnection = htspConnection;
        }

        @Override
        public DataSource createDataSource() {
            Timber.d("Created new data source from factory");
            dataSource = new HtspFileInputStreamDataSource(context, htspConnection);
            return dataSource;
        }

        HtspFileInputStreamDataSource getCurrentDataSource() {
            Timber.d("Returning data source");
            return dataSource;
        }

        void releaseCurrentDataSource() {
            Timber.d("Releasing data source");
            if (dataSource != null) {
                dataSource.release();
            }
        }
    }

    private HtspFileInputStreamDataSource(Context mContext, HtspConnection htspConnection) {
        Timber.d("Initializing file input data source");
        this.context = mContext;
        this.htspConnection = htspConnection;
        this.htspConnection.addMessageListener(this);
        this.dataSourceNumber = dataSourceCount.incrementAndGet();
    }

    @Override
    public long getTimeshiftOffsetPts() {
        return Long.MIN_VALUE;
    }

    @Override
    public void setSpeed(int tvhSpeed) {

    }

    @Override
    public long getTimeshiftStartTime() {
        return Long.MIN_VALUE;
    }

    @Override
    public long getTimeshiftStartPts() {
        return Long.MIN_VALUE;
    }

    @Override
    public void resume() {
        // No action needed
    }

    @Override
    public void pause() {
        // No action needed
    }

    @Override
    protected void finalize() throws Throwable {
        Timber.d("Finalizing file input data source");
        release();
        super.finalize();
    }

    @Override
    public long open(DataSpec dataSpec) {
        Timber.d("Opening file input data source " + dataSourceNumber + ")");
        this.dataSpec = dataSpec;

        fileName = "dvrfile" + dataSpec.uri.getPath();

        HtspMessage fileReadRequest = new HtspMessage();
        fileReadRequest.put("method", "fileRead");
        fileReadRequest.put("size", 1024000);

        final HtspResponseListener fileReadHandler = response -> {
            if (response.containsKey("error")) {
                String error = response.getString("error");
                Timber.d("Error reading file at offset 0: " + error);

            } else {
                final byte[] data = response.getByteArray("data");
                Timber.d("Fetched " + data.length + " bytes of file at offset 0");
                filePosition += data.length;
                byteBuffer = ByteBuffer.wrap(data);
            }
            synchronized (fileReadRequest) {
                Timber.d("Notifying fileReadRequest");
                fileReadRequest.notify();
            }
        };

        HtspMessage fileOpenRequest = new HtspMessage();
        fileOpenRequest.put("method", "fileOpen");
        fileOpenRequest.put("file", fileName);
        htspConnection.sendMessage(fileOpenRequest, response -> {
            if (response.containsKey("error")) {
                String error = response.getString("error");
                Timber.d("Error opening file: " + error);

            } else {
                Timber.d("Opening file: " + fileName);
                fileId = response.getInteger("id");
                if (response.containsKey("size")) {
                    fileSize = response.getLong("size");
                    Timber.v("Opened file " + fileName + " of size " + fileSize + " successfully");
                } else {
                    Timber.v("Opened file " + fileName + " successfully");
                }
                Timber.d("Sending file read request for file id " + fileId);
                fileReadRequest.put("id", fileId);
                htspConnection.sendMessage(fileReadRequest, fileReadHandler);
            }
        });

        Timber.d("Waiting for fileReadRequest");
        synchronized (fileReadRequest) {
            try {
                fileReadRequest.wait(5000);
            } catch (InterruptedException e) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted");
            }
        }

        Timber.d("Opened file " + fileName + ", id " + fileId + " with size " + fileSize);
        return fileSize;
    }

    @Override
    public int read(byte[] bytes, int offset, int readLength) {

        // If we've reached the end of the file, we're done :)
        if (fileSize == filePosition && !byteBuffer.hasRemaining()) {
            return -1;
        }

        sendFileRead(filePosition);

        if (!byteBuffer.hasRemaining() && fileSize == -1) {
            // If we still don't have any data, and we
            // don't have a known size, then we're done.
            return -1;

        } else if (!byteBuffer.hasRemaining()) {
            // If we don't have data here, something went wrong
            Timber.d("Failed to read data for " + fileName);
        }

        int startPos = byteBuffer.position();

        byteBuffer.get(bytes, offset, Math.min(readLength, byteBuffer.remaining()));
        return byteBuffer.position() - startPos;
    }

    @Override
    public Uri getUri() {
        Timber.d("Returning data spec uri");
        if (dataSpec != null) {
            return dataSpec.uri;
        }
        return null;
    }

    @Override
    public void close() {
        Timber.d("Closing file input data source " + dataSourceNumber + ")");
    }

    @Override
    public void onMessage(@NotNull HtspMessage response) {

    }

    // HtspDataSource Methods
    private void release() {
        Timber.d("Releasing file input data source " + dataSourceNumber + ")");

        HtspMessage request = new HtspMessage();
        request.put("method", "fileClose");
        request.put("id", fileId);
        htspConnection.sendMessage(request, null);
        htspConnection.removeMessageListener(this);

        // Watch for memory leaks
        MainApplication.refWatcher.watch(this);
    }

    private void sendFileRead(long offset) {
        Timber.d("Sending message to read file from offset " + offset);
        if (byteBuffer != null && byteBuffer.hasRemaining()) {
            Timber.d("Buffer is not null and has elements remaining");
            return;
        }

        long size = 1024000;
        if (fileSize != -1) {
            // Make sure we don't overrun the file
            if (offset + size > fileSize) {
                size = fileSize - offset;
            }
        }

        HtspMessage request = new HtspMessage();
        request.put("method", "fileRead");
        request.put("id", fileId);
        request.put("size", size);
        request.put("offset", offset);

        Timber.d("Fetching " + size + " bytes of file at offset " + offset);
        htspConnection.sendMessage(request, response -> {
            if (response.containsKey("error")) {
                String error = response.getString("error");
                Timber.d("Error reading file at " + offset + ": " + error);

            } else {
                final byte[] data = response.getByteArray("data");
                Timber.d("Fetched " + data.length + " bytes of file at offset " + offset);
                filePosition += data.length;
                byteBuffer = ByteBuffer.wrap(data);
            }
            synchronized (request) {
                request.notify();
            }
        });

        Timber.d("Waiting for file read request");
        synchronized (request) {
            try {
                request.wait(5000);
            } catch (InterruptedException e) {
                Timber.d(e, "Waiting for fileReadRequest message was interrupted.");
            }
        }
    }
}
