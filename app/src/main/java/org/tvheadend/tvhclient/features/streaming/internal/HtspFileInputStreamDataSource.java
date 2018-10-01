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

package org.tvheadend.tvhclient.features.streaming.internal;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSpec;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.service.htsp.HtspFileInputStream;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class HtspFileInputStreamDataSource extends HtspDataSource {

    private static final AtomicInteger sDataSourceCount = new AtomicInteger();

    public static class Factory extends HtspDataSource.Factory {

        private final Context mContext;
        private final SimpleHtspConnection mConnection;

        public Factory(Context context, SimpleHtspConnection connection) {
            mContext = context;
            mConnection = connection;
        }

        @Override
        public HtspDataSource createDataSourceInternal() {
            return new HtspFileInputStreamDataSource(mContext, mConnection);
        }

    }

    private final int mDataSourceNumber;
    private HtspFileInputStream mHtspFileInputStream;

    private HtspFileInputStreamDataSource(Context context, SimpleHtspConnection connection) {
        super(context, connection);

        mDataSourceNumber = sDataSourceCount.incrementAndGet();

        Timber.d("New HtspSubscriptionDataSource instantiated (" + mDataSourceNumber + ")");
    }

    @Override
    protected void finalize() throws Throwable {
        // This is a total hack, but there's not much else we can do?
        // https://github.com/google/ExoPlayer/issues/2662 - Luckily, i've not found it's actually
        // been used anywhere at this moment.
        if (mConnection != null) {
            Timber.e("Datasource finalize relied upon to release the subscription");
            release();
        }

        super.finalize();
    }

    // DataSource Methods
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Timber.i("Opening HTSP DataSource (" + mDataSourceNumber + ")");

        mDataSpec = dataSpec;

        String fileName = "dvrfile" + dataSpec.uri.getPath();

        mHtspFileInputStream = new HtspFileInputStream(mConnection, fileName);

        return mHtspFileInputStream.getFileSize();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        return mHtspFileInputStream.read(buffer, offset, readLength);
    }

    @Override
    public void close() throws IOException {
        Timber.i("Closing HTSP DataSource (" + mDataSourceNumber + ")");
        if (mHtspFileInputStream != null) {
            mHtspFileInputStream.close();
        }
    }

    // HtspDataSource Methods
    protected void release() {
        if (mConnection != null) {
            mConnection = null;
        }

        if (mHtspFileInputStream != null) {
            try {
                mHtspFileInputStream.close();
            } catch (IOException e) {
                // Ignore.
            }
            mHtspFileInputStream = null;
        }

        // Watch for memory leaks
        MainApplication.getRefWatcher(mContext).watch(this);
    }

    @Override
    public void pause() {
        // No action needed
    }

    @Override
    public void resume() {
        // No action needed
    }

    @Override
    public long getTimeshiftStartTime() {
        // No action needed?
        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public long getTimeshiftStartPts() {
        // No action needed?
        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public long getTimeshiftOffsetPts() {
        // No action needed?
        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public void setSpeed(int speed) {
        // No action needed
    }
}
