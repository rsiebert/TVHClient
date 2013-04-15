package org.tvheadend.tvhguide.htsp;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class HTSFileInputStream extends InputStream {
	private HTSConnection connection;
	private String path;

	private long fileId;
	private long fileSize;

	private byte[] buf;
	private int bufPos;
	private long offset;

	public HTSFileInputStream(HTSConnection conn, String path)
			throws IOException {
		this.connection = conn;
		this.path = path;

		this.fileId = -1;
		this.fileSize = -1;

		this.reset();
		this.open();
	}

	public int available() {
		return this.buf.length;
	}

	public boolean markSupported() {
		return false;
	}

	public void reset() {
		buf = new byte[0];
		bufPos = 0;
		offset = 0;
	}

	class FileOpenResponse implements HTSResponseHandler {
		int id;
		long size;
		long mtime;

		@Override
		public void handleResponse(HTSMessage response) {
			id = response.getInt("id", 0);
			size = response.getLong("size", 0);
			mtime = response.getLong("mtime", 0);
			notifyAll();
		}
	};

	class FileReadResponse implements HTSResponseHandler {
		byte[] data;

		@Override
		public void handleResponse(HTSMessage response) {
			data = response.getByteArray("data");
			notifyAll();
		}
	};

	class FileCloseResponse implements HTSResponseHandler {
		int id;

		@Override
		public void handleResponse(HTSMessage response) {
			notifyAll();
		}
	};

	private void open() throws IOException {
		HTSMessage request = new HTSMessage();
		FileOpenResponse response = new FileOpenResponse();

		request.setMethod("fileOpen");
		request.putField("file", path);

		synchronized (response) {
			try {
				connection.sendMessage(request, response);
				response.wait();
				fileId = response.id;
				fileSize = response.size;
			} catch (Throwable e) {
				Log.e("TVHGuide", "Timeout waiting for fileOpen", e);
			}
		}

		if (fileId < 0) {
			throw new IOException("Failed to open remote file");
		} else if (fileId == 0) {
			throw new IOException("Remote file is missing");
		}
	}

	public void close() {
		HTSMessage request = new HTSMessage();
		FileCloseResponse response = new FileCloseResponse();

		request.setMethod("fileClose");
		request.putField("id", fileId);

		synchronized (response) {
			try {
				connection.sendMessage(request, response);
				response.wait();
				fileId = -1;
				fileSize = -1;
			} catch (Throwable e) {
				Log.e("TVHGuide", "Timeout waiting for fileClose", e);
			}
		}
	}

	public int read(byte[] outBuf, int outOffset, int outLength) {
		fillBuffer();
		
		int ret = Math.min(buf.length - bufPos, outLength - outOffset);
		if(ret > 0) {
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

	private void fillBuffer() {
		if(bufPos < buf.length) {
			return;
		}
		
		HTSMessage request = new HTSMessage();
		FileReadResponse response = new FileReadResponse();

		request.setMethod("fileRead");
		request.putField("id", fileId);
		request.putField("size", Math.min(fileSize, 1024 * 1024 * 8));
		request.putField("offset", offset);

		synchronized (response) {
			try {
				connection.sendMessage(request, response);
				response.wait();

				offset += buf.length;
				buf = response.data;
				bufPos = 0;
			} catch (Throwable e) {
				Log.e("TVHGuide", "Timeout waiting for fileRead", e);
			}
		}
	}
}
