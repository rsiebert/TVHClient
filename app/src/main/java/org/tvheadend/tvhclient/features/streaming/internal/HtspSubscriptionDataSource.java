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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.data.service.htsp.HtspNotConnectedException;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Subscriber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

public class HtspSubscriptionDataSource extends HtspDataSource implements Subscriber.Listener {
    private static final AtomicInteger sDataSourceCount = new AtomicInteger();
    private static final int BUFFER_SIZE = 10 * 1024 * 1024;
    static final byte[] HEADER = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};

    public static class Factory extends HtspDataSource.Factory {

        private final Context mContext;
        private final SimpleHtspConnection mConnection;
        private final String mStreamProfile;

        public Factory(Context context, SimpleHtspConnection connection, String streamProfile) {
            mContext = context;
            mConnection = connection;
            mStreamProfile = streamProfile;
        }

        @Override
        public HtspDataSource createDataSourceInternal() {
            return new HtspSubscriptionDataSource(mContext, mConnection, mStreamProfile);
        }
    }

    private final String mStreamProfile;

    private int mTimeshiftPeriod = 0;

    private final int mDataSourceNumber;
    private Subscriber mSubscriber;

    private ByteBuffer mBuffer;
    private final ReentrantLock mLock = new ReentrantLock();

    private boolean mIsOpen = false;
    private boolean mIsSubscribed = false;

    private HtspSubscriptionDataSource(Context context, SimpleHtspConnection connection, String streamProfile) {
        super(context, connection);

        mStreamProfile = streamProfile;

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        boolean timeshiftEnabled = mSharedPreferences.getBoolean(
                "timeshift_enabled",
                false);

        if (timeshiftEnabled) {
            // TODO: Eventually, this should be a preference.
            mTimeshiftPeriod = 3600;
        }

        mDataSourceNumber = sDataSourceCount.incrementAndGet();

        Timber.d("New HtspSubscriptionDataSource instantiated (" + mDataSourceNumber + ")");

        try {
            // Create the buffer, and place the HtspSubscriptionDataSource header in place.
            mBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            mBuffer.limit(HEADER.length);
            mBuffer.put(HEADER);
            mBuffer.position(0);
        } catch (OutOfMemoryError e) {
            // Since we're allocating a large buffer here, it's fairly safe to assume we'll have
            // enough memory to catch and throw this exception. We do this, as each OOM exception
            // message is unique (lots of #'s of bytes available/used/etc) and means crash reporting
            // doesn't group things nicely.
            throw new RuntimeException("OutOfMemoryError when allocating HtspSubscriptionDataSource buffer (" + mDataSourceNumber + ")", e);
        }

        mSubscriber = new Subscriber(mConnection);
        mSubscriber.addSubscriptionListener(this);
        mConnection.addAuthenticationListener(mSubscriber);
    }

    @Override
    protected void finalize() throws Throwable {
        // This is a total hack, but there's not much else we can do?
        // https://github.com/google/ExoPlayer/issues/2662 - Luckily, i've not found it's actually
        // been used anywhere at this moment.
        if (mSubscriber != null || mConnection != null) {
            Timber.e("Datasource finalize relied upon to release the subscription");
            release();
        }

        super.finalize();
    }

    // DataSource Methods
    @Override
    public long open(DataSpec dataSpec) throws IOException {
        Timber.i("Opening HtspSubscriptionDataSource (" + mDataSourceNumber + ")");
        mDataSpec = dataSpec;

        if (!mIsSubscribed) {
            try {
                String path = dataSpec.uri.getPath();
                if (!TextUtils.isEmpty(path)) {
                    long channelId = Long.parseLong(path.substring(1));
                    mSubscriber.subscribe(channelId, mStreamProfile, mTimeshiftPeriod);
                    mIsSubscribed = true;
                }
            } catch (HtspNotConnectedException e) {
                throw new IOException("Failed to startPlayback HtspSubscriptionDataSource, HTSP not connected (" + mDataSourceNumber + ")", e);
            }
        }

        long seekPosition = mDataSpec.position;
        if (seekPosition > 0 && mTimeshiftPeriod > 0) {
            Timber.d("Seek to time PTS: " + seekPosition);

            mSubscriber.skip(seekPosition);
            mBuffer.clear();
            mBuffer.limit(0);
        }

        mIsOpen = true;

        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        if (readLength == 0) {
            return 0;
        }

        // If the buffer is empty, block until we have at least 1 byte
        while (mIsOpen && mBuffer.remaining() == 0) {
            try {
                Timber.v("Blocking for more data (" + mDataSourceNumber + ")");
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // Ignore.
                Timber.w("Caught InterruptedException (" + mDataSourceNumber + ")");
                return 0;
            }
        }

        if (!mIsOpen && mBuffer.remaining() == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int length;

        mLock.lock();
        try {
            int remaining = mBuffer.remaining();
            length = remaining >= readLength ? readLength : remaining;

            mBuffer.get(buffer, offset, length);
            mBuffer.compact();
            mBuffer.flip();
        } finally {
            mLock.unlock();
        }

        return length;
    }

    @Override
    public void close() {
        Timber.i("Closing HTSP DataSource (" + mDataSourceNumber + ")");
        mIsOpen = false;
    }

    // Subscription.Listener Methods
    @Override
    public void onSubscriptionStart(@NonNull HtspMessage message) {
        Timber.d("Received subscriptionStart (" + mDataSourceNumber + ")");
        serializeMessageToBuffer(message);
    }

    @Override
    public void onSubscriptionStatus(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onSubscriptionSkip(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onSubscriptionSpeed(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onSubscriptionStop(@NonNull HtspMessage message) {
        Timber.d("Received subscriptionStop (" + mDataSourceNumber + ")");
        mIsOpen = false;
    }

    @Override
    public void onQueueStatus(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onSignalStatus(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onTimeshiftStatus(@NonNull HtspMessage message) {
        // Don't care about this event here
    }

    @Override
    public void onMuxpkt(@NonNull HtspMessage message) {
        serializeMessageToBuffer(message);
    }

    // HtspDataSource Methods
    protected void release() {
        if (mConnection != null) {
            mConnection.removeAuthenticationListener(mSubscriber);
            mConnection = null;
        }

        if (mSubscriber != null) {
            mSubscriber.removeSubscriptionListener(this);
            mSubscriber.unsubscribe();
            mSubscriber = null;
        }

        // Watch for memory leaks
        MainApplication.getRefWatcher(mContext).watch(this);
    }

    @Override
    public void pause() {
        if (mSubscriber != null) {
            mSubscriber.pause();
        }
    }

    @Override
    public void resume() {
        if (mSubscriber != null) {
            mSubscriber.resume();
        }
    }

    @Override
    public long getTimeshiftStartTime() {
        if (mSubscriber != null) {
            return mSubscriber.getTimeshiftStartTime();
        }

        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public long getTimeshiftStartPts() {
        if (mSubscriber != null) {
            return mSubscriber.getTimeshiftStartPts();
        }

        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public long getTimeshiftOffsetPts() {
        if (mSubscriber != null) {
            return mSubscriber.getTimeshiftOffsetPts();
        }

        return INVALID_TIMESHIFT_TIME;
    }

    @Override
    public void setSpeed(int speed) {
        if (mSubscriber != null) {
            mSubscriber.setSpeed(speed);
        }
    }

    // Misc Internal Methods
    private void serializeMessageToBuffer(@NonNull HtspMessage message) {

        mLock.lock();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
            objectOutput.writeUnshared(message);
            objectOutput.flush();

            mBuffer.position(mBuffer.limit());
            mBuffer.limit(mBuffer.capacity());

            mBuffer.put(outputStream.toByteArray());

            mBuffer.flip();
        } catch (IOException e) {
            // Ignore?
            Timber.w("Caught IOException, ignoring (" + mDataSourceNumber + ")", e);
        } catch (BufferOverflowException boe) {
            Timber.w("Caught BufferOverflowException, ignoring (" + mDataSourceNumber + ")", boe);
        } finally {
            mLock.unlock();
            // Ignore
        }
    }
}
