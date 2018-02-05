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

package org.tvheadend.tvhclient.service.htsp;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * Fetches a file over a HTSP Connection
 */
public class HtspFileInputStream extends InputStream {
    private static final String TAG = HtspFileInputStream.class.getSimpleName();

    private final HtspMessage.Dispatcher dispatcher;
    private final String fileName;

    private ByteBuffer buffer;

    private int fileId = -1;
    private long fileSize = -1;
    private long filePosition = 0;

    public HtspFileInputStream(@NonNull HtspMessage.Dispatcher dispatcher, String fileName) throws IOException {
        this.dispatcher = dispatcher;
        this.fileName = fileName;

        Log.i(TAG, "Opening HtspFileInputStream for " + this.fileName);

        sendFileOpen();
        sendFileRead(1024000, 0);
    }

    public long getFileSize() {
        return fileSize;
    }

    // InputStream Methods
    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     * <p>
     * <p> A subclass must provide an implementation of this method.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        // If we've reached the end of the file, we're done :)
        if (fileSize == filePosition && !buffer.hasRemaining()) {
            return -1;
        }

        sendFileRead(1024000, filePosition);

        if (!buffer.hasRemaining() && fileSize == -1) {
            // If we still don't have any data, and we don't have a known size, then we're done.
            return -1;

        } else if (!buffer.hasRemaining()) {
            // If we don't have data here, something went wrong
            throw new IOException("Failed to read data for " + fileName);
        }

        return buffer.get() & 0xff;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     * <p>
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     * <p>
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     * <p>
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     * <p>
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     * <p>
     * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method
     * for class <code>InputStream</code> simply calls the method
     * <code>read()</code> repeatedly. If the first such call results in an
     * <code>IOException</code>, that exception is returned from the call to
     * the <code>read(b,</code> <code>off,</code> <code>len)</code> method.  If
     * any subsequent call to <code>read()</code> results in a
     * <code>IOException</code>, the exception is caught and treated as if it
     * were end of file; the bytes read up to that point are stored into
     * <code>b</code> and the number of bytes read before the exception
     * occurred is returned. The default implementation of this method blocks
     * until the requested amount of input data <code>len</code> has been read,
     * end of file is detected, or an exception is thrown. Subclasses are encouraged
     * to provide a more efficient implementation of this method.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in array <code>b</code>
     *            at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     * @throws IOException               If the first byte cannot be read for any reason
     *                                   other than end of file, or if the input stream has been closed, or if
     *                                   some other I/O error occurs.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @see InputStream#read()
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // If we've reached the end of the file, we're done :)
        if (fileSize == filePosition && !buffer.hasRemaining()) {
            return -1;
        }

        sendFileRead(1024000, filePosition);

        if (!buffer.hasRemaining() && fileSize == -1) {
            // If we still don't have any data, and we don't have a known size, then we're done.
            return -1;

        } else if (!buffer.hasRemaining()) {
            // If we don't have data here, something went wrong
            throw new IOException("Failed to read data for " + fileName);
        }

        int startPos = buffer.position();

        buffer.get(b, off, Math.min(len, buffer.remaining()));

        return buffer.position() - startPos;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        sendFileClose();
    }

    // Internal Methods
    private void sendFileOpen() throws IOException {
        HtspMessage fileOpenRequest = new HtspMessage();

        fileOpenRequest.put("method", "fileOpen");
        fileOpenRequest.put("file", fileName);

        HtspMessage fileOpenResponse;
        try {
            fileOpenResponse = dispatcher.sendMessage(fileOpenRequest, 5000);
        } catch (HtspNotConnectedException e) {
            throw new IOException("Failed to send fileOpen request", e);
        }

        if (fileOpenResponse == null) {
            throw new IOException("Failed to receive response to fileOpen request");
        } else if (fileOpenResponse.containsKey("error")) {
            String error = fileOpenResponse.getString("error");
            Log.e(TAG, "Received error when opening file: " + error);
            throw new FileNotFoundException(error);
        }

        fileId = fileOpenResponse.getInteger("id");

        if (fileOpenResponse.containsKey("size")) {
            // Size is optional
            fileSize = fileOpenResponse.getLong("size");
            Log.v(TAG, "Opened file " + fileName + " of size " + fileSize + " successfully");
        } else {
            Log.v(TAG, "Opened file " + fileName + " successfully");
        }
    }

    private void sendFileRead(long size, long offset) throws IOException {
        if (buffer != null && buffer.hasRemaining()) {
            return;
        }

        if (fileSize != -1) {
            // Make sure we don't overrun the file
            if (offset + size > fileSize) {
                size = fileSize - offset;
            }
        }

        HtspMessage fileReadRequest = new HtspMessage();

        fileReadRequest.put("method", "fileRead");
        fileReadRequest.put("id", fileId);
        fileReadRequest.put("size", size);
        fileReadRequest.put("offset", offset);

        if (HtspConstants.DEBUG)
            Log.v(TAG, "Fetching " + size + " bytes of file at offset " + offset);

        HtspMessage fileReadResponse;
        try {
            fileReadResponse = dispatcher.sendMessage(fileReadRequest, 5000);
        } catch (HtspNotConnectedException e) {
            throw new IOException("Failed to send fileRead request", e);
        }

        if (fileReadResponse == null) {
            throw new IOException("Failed to receive response to fileRead request");
        } else if (fileReadResponse.containsKey("error")) {
            String error = fileReadResponse.getString("error");
            Log.e(TAG, "Received error when reading file: " + error);
            throw new IOException(error);
        }

        final byte[] data = fileReadResponse.getByteArray("data");

        if (HtspConstants.DEBUG)
            Log.v(TAG, "Fetched " + data.length + " bytes of file at offset " + offset);

        filePosition += data.length;
        buffer = ByteBuffer.wrap(data);
    }

    private void sendFileClose() throws IOException {
        Log.v(TAG, "Closing file " + fileName);

        HtspMessage fileCloseRequest = new HtspMessage();

        fileCloseRequest.put("method", "fileClose");
        fileCloseRequest.put("id", fileId);

        // We just go ahead and send the close without waiting for a response, if it fails, oh well.
        try {
            dispatcher.sendMessage(fileCloseRequest);
        } catch (HtspNotConnectedException e) {
            throw new IOException("Failed to send fileClose request", e);
        }
    }
}
