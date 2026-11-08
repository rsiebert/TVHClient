package org.tvheadend.htsp;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.tvheadend.api.ServerResponseListener;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class HtspFileInputStream extends InputStream {
    private static final long RESPONSE_TIMEOUT_MS = 10_000;

    /**
     * How long a request waits for its response before giving up. Package-private and mutable so
     * that tests can lower it; production code always uses {@link #RESPONSE_TIMEOUT_MS}.
     */
    static volatile long responseTimeoutMs = RESPONSE_TIMEOUT_MS;

    private final HtspConnection connection;
    private final String path;

    private long fileId;
    private long fileSize;

    private byte[] buf;
    private int bufPos;
    private long offset;

    public HtspFileInputStream(HtspConnection conn, String path) throws IOException {
        this.connection = conn;
        this.path = path;

        this.fileId = -1;
        this.fileSize = -1;

        this.reset();
        this.open();
    }

    public int available() {
        // Only the bytes that have not been handed out yet are available. Returning the whole
        // fetched chunk over-reports as soon as the caller has consumed part of it.
        return this.buf.length - this.bufPos;
    }

    public boolean markSupported() {
        return false;
    }

    public void reset() {
        buf = new byte[0];
        bufPos = 0;
        offset = 0;
    }

    static class FileOpenResponse implements ServerResponseListener<HtspMessage> {
        int id;
        long size;
        long time;
        boolean responded;

        @Override
        public void handleResponse(@NotNull HtspMessage response) {
            id = response.getInteger("id", 0);
            size = response.getLong("size", 0);
            time = response.getLong("mtime", 0);
            responded = true;
            notifyAll();
        }
    }

    static class FileReadResponse implements ServerResponseListener<HtspMessage> {
        byte[] data;
        boolean responded;

        @Override
        public void handleResponse(@NotNull HtspMessage response) {
            data = response.getByteArray("data");
            responded = true;
            notifyAll();
        }
    }

    static class FileCloseResponse implements ServerResponseListener<HtspMessage> {
        boolean responded;

        @Override
        public void handleResponse(@NotNull HtspMessage response) {
            responded = true;
            notifyAll();
        }
    }

    private void open() throws IOException {
        HtspMessage request = new HtspMessage();
        FileOpenResponse response = new FileOpenResponse();

        request.setMethod("fileOpen");
        request.put("file", path);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait(responseTimeoutMs);
                fileId = response.id;
                fileSize = response.size;
            } catch (Throwable e) {
                Timber.e(e, "Timeout waiting for fileOpen");
            }
        }

        // A reply that never arrived must surface as an error instead of being mistaken for a
        // missing file: after a timeout fileId is still at its default of 0, not below it.
        if (!response.responded) {
            throw new IOException("Timeout waiting for the fileOpen response");
        }

        if (fileId < 0) {
            throw new IOException("Failed to open remote file");
        } else if (fileId == 0) {
            throw new IOException("Remote file is missing");
        }
    }

    public void close() {
        HtspMessage request = new HtspMessage();
        FileCloseResponse response = new FileCloseResponse();

        request.setMethod("fileClose");
        request.put("id", fileId);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait(responseTimeoutMs);
                fileId = -1;
                fileSize = -1;
            } catch (Throwable e) {
                Timber.e(e, "Timeout waiting for fileClose");
            }
        }

        // close() must never throw, so a server that never answers is only logged.
        if (!response.responded) {
            Timber.e("Timeout waiting for the fileClose response");
        }
    }

    public int read(@NonNull byte[] outBuf, int outOffset, int outLength) throws IOException {
        fillBuffer();

        // outOffset is where to start writing into outBuf, not a limit on how much to read: the
        // InputStream contract is to read at most outLength bytes. Using outLength - outOffset made
        // every caller that passed an off > 0 receive fewer bytes than it asked for.
        int ret = Math.min(buf.length - bufPos, outLength);
        if (ret > 0) {
            System.arraycopy(buf, bufPos, outBuf, outOffset, ret);
            bufPos += ret;
            return ret;
        }

        return -1;
    }

    @Override
    public int read() throws IOException {
        fillBuffer();

        if (bufPos < buf.length) {
            return buf[bufPos++] & 0xff;
        }

        return -1;
    }

    private void fillBuffer() throws IOException {
        if (bufPos < buf.length) {
            return;
        }

        HtspMessage request = new HtspMessage();
        FileReadResponse response = new FileReadResponse();

        request.setMethod("fileRead");
        request.put("id", fileId);
        request.put("size", Math.min(fileSize, 1024 * 1024 * 8));
        request.put("offset", offset);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait(responseTimeoutMs);

                if (response.responded) {
                    // Advance past the chunk just received. This previously added the *previous*
                    // buf.length before reassigning buf, so on the first call offset stayed 0 (buf was
                    // still the empty array from reset()) and the first chunk was fetched twice.
                    buf = response.data;
                    offset += buf.length;
                    bufPos = 0;
                }
            } catch (Throwable e) {
                Timber.e(e, "Timeout waiting for fileRead");
            }
        }

        // A stalled server must not look like the end of the stream, so surface the timeout.
        if (!response.responded) {
            throw new IOException("Timeout waiting for the fileRead response");
        }
    }
}
