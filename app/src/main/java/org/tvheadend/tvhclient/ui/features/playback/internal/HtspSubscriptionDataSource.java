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

package org.tvheadend.tvhclient.ui.features.playback.internal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.jetbrains.annotations.NotNull;
import org.tvheadend.htsp.HtspConnection;
import org.tvheadend.htsp.HtspMessage;
import org.tvheadend.htsp.HtspMessageListener;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.HtspService;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import leakcanary.LeakSentry;
import timber.log.Timber;

public class HtspSubscriptionDataSource implements DataSource, Closeable, HtspMessageListener, HtspDataSourceInterface {

    private static final AtomicInteger dataSourceCount = new AtomicInteger();
    private static final AtomicInteger subscriptionCount = new AtomicInteger();

    private static final int BUFFER_SIZE = 10 * 1024 * 1024;
    static final byte[] HEADER = new byte[]{0, 1, 0, 1, 0, 1, 0, 1};

    private final Context context;
    private final HtspConnection htspConnection;
    private final String streamProfile;
    private DataSpec dataSpec;
    private final int dataSourceNumber;
    private final int subscriptionId;
    private ByteBuffer byteBuffer;
    private int timeshiftPeriod = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean subscriptionStarted = false;
    private boolean isSubscribed = false;

    public static class Factory implements DataSource.Factory {

        private final Context context;
        private final HtspConnection htspConnection;
        private final String streamProfile;
        private HtspSubscriptionDataSource dataSource;

        Factory(Context context, HtspConnection htspConnection, String streamProfile) {
            Timber.d("Initializing subscription data source factory");
            this.context = context;
            this.htspConnection = htspConnection;
            this.streamProfile = streamProfile;
        }

        @Override
        public DataSource createDataSource() {
            Timber.d("Created new data source from factory");
            dataSource = new HtspSubscriptionDataSource(context, htspConnection, streamProfile);
            return dataSource;
        }

        HtspDataSourceInterface getCurrentDataSource() {
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

    private HtspSubscriptionDataSource(Context context, HtspConnection htspConnection, String streamProfile) {
        Timber.d("Initializing subscription data source");
        this.context = context;
        this.htspConnection = htspConnection;
        this.htspConnection.addMessageListener(this);
        this.streamProfile = streamProfile;

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean timeshiftEnabled = mSharedPreferences.getBoolean("timeshift_enabled", context.getResources().getBoolean(R.bool.pref_default_timeshift_enabled));
        if (timeshiftEnabled) {
            // TODO: Eventually, this should be a preference.
            timeshiftPeriod = 3600;
        }

        dataSourceNumber = dataSourceCount.incrementAndGet();
        subscriptionId = subscriptionCount.incrementAndGet();

        Timber.d("New subscription data source instantiated (" + dataSourceNumber + ")");

        try {
            // Create the buffer, and place the HtspSubscriptionDataSource header in place.
            byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            byteBuffer.limit(HEADER.length);
            byteBuffer.put(HEADER);
            byteBuffer.position(0);

        } catch (OutOfMemoryError e) {
            // Since we're allocating a large buffer here, it's fairly safe to assume we'll have
            // enough memory to catch and throw this exception. We do this, as each OOM exception
            // message is unique (lots of #'s of bytes available/used/etc) and means crash reporting
            // doesn't group things nicely.
            throw new RuntimeException("OutOfMemoryError when allocating subscription data source buffer (" + dataSourceNumber + ")", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        Timber.d("Finalizing subscription data source");
        release();
        super.finalize();
    }

    @Override
    public long open(DataSpec dataSpec) {
        Timber.d("Opening subscription data source " + dataSourceNumber + ")");
        this.dataSpec = dataSpec;

        if (!isSubscribed) {
            String path = dataSpec.uri.getPath();
            Timber.d("We are not yet subscribed to path %s", path);
            if (path != null && path.length() > 0 ) {

                int channelId = Integer.parseInt(path.substring(1));
                Timber.d("Sending subscription start to service with id " + subscriptionId + " for channel id " + channelId);

                HtspMessage request = new HtspMessage();
                request.setMethod("subscribe");
                request.put("subscriptionId", subscriptionId);
                request.put("channelId", channelId);
                request.put("timeshiftPeriod", timeshiftPeriod);

                if (!TextUtils.isEmpty(streamProfile)) {
                    request.put("profile", streamProfile);
                }

                htspConnection.sendMessage(request, response -> {
                    Timber.d("Received subscribe response");
                    int availableTimeshiftPeriod = response.getInteger("timeshiftPeriod", 0);
                    Timber.d("Available timeshift period in seconds: %s", availableTimeshiftPeriod);
                });
                isSubscribed = true;
            }
        }

        Timber.d("Getting seek position");
        long seekPosition = this.dataSpec.position;
        if (seekPosition > 0 && timeshiftPeriod > 0) {
            Timber.d("Sending subscription skip to server with id " + subscriptionId + " with time PTS: " + seekPosition);

            HtspMessage request = new HtspMessage();
            request.put("method", "subscriptionSkip");
            request.put("subscriptionId", subscriptionId);
            request.put("time", seekPosition);
            request.put("absolute", 1);

            htspConnection.sendMessage(request, null);

            byteBuffer.clear();
            byteBuffer.limit(0);
        }

        subscriptionStarted = true;

        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        if (readLength == 0) {
            return 0;
        }

        // If the buffer is empty, block until we have at least 1 byte
        while (subscriptionStarted && byteBuffer.remaining() == 0) {
            try {
                Timber.v("Blocking for more data (" + dataSourceNumber + ")");
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // Ignore.
                Timber.w("Caught InterruptedException (" + dataSourceNumber + ")");
                return 0;
            }
        }

        if (!subscriptionStarted && byteBuffer.remaining() == 0) {
            Timber.d("End of input buffer");
            return C.RESULT_END_OF_INPUT;
        }

        int length;

        lock.lock();
        try {
            int remaining = byteBuffer.remaining();
            length = remaining >= readLength ? readLength : remaining;

            byteBuffer.get(buffer, offset, length);
            byteBuffer.compact();
            byteBuffer.flip();
        } finally {
            lock.unlock();
        }

        return length;
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
        Timber.d("Closing subscription data source " + dataSourceNumber + ")");
        subscriptionStarted = false;
    }

    @Override
    public void onMessage(@NotNull HtspMessage message) {
        String method = message.getMethod();
        switch (method) {
            case "subscriptionStart":
            case "muxpkt":
                serializeMessageToBuffer(message);
                break;

            case "subscriptionStop":
                subscriptionStarted = false;
                break;

            case "subscriptionStatus":
            case "subscriptionSkip":
            case "subscriptionSpeed":
            case "queueStatus":
            case "signalStatus":
            case "timeshiftStatus":
                break;

            default:
                break;
        }
    }

    private void release() {
        Timber.d("Releasing subscription data source " + dataSourceNumber + ")");

        HtspMessage request = new HtspMessage();
        request.put("method", "unsubscribe");
        request.put("subscriptionId", subscriptionId);
        htspConnection.sendMessage(request, null);
        htspConnection.removeMessageListener(this);

        // Watch for memory leaks
        LeakSentry.INSTANCE.getRefWatcher().watch(this);
    }

    public void pause() {
        Timber.d("Pausing subscription data source " + dataSourceNumber + ")");

        HtspMessage request = new HtspMessage();
        request.put("method", "subscriptionSpeed");
        request.put("subscriptionId", subscriptionId);
        request.put("speed", 0);
        htspConnection.sendMessage(request, null);
    }

    @Override
    public long getTimeshiftOffsetPts() {
        return 0;
    }

    @Override
    public void setSpeed(int tvhSpeed) {
        HtspMessage request = new HtspMessage();
        request.put("method", "subscriptionSpeed");
        request.put("subscriptionId", subscriptionId);
        request.put("speed", tvhSpeed);
        htspConnection.sendMessage(request, null);
    }

    @Override
    public long getTimeshiftStartTime() {
        return 0;
    }

    @Override
    public long getTimeshiftStartPts() {
        return 0;
    }

    public void resume() {
        Timber.d("Resuming subscription data source " + dataSourceNumber + ")");
        Intent intent = new Intent(context, HtspService.class);
        intent.putExtra("method", "subscriptionSpeed");
        intent.putExtra("subscriptionId", subscriptionId);
        intent.putExtra("speed", 100);
        context.startService(intent);
    }

    // Misc Internal Methods
    private void serializeMessageToBuffer(@NonNull HtspMessage message) {
        lock.lock();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
            objectOutput.writeUnshared(message);
            objectOutput.flush();

            byteBuffer.position(byteBuffer.limit());
            byteBuffer.limit(byteBuffer.capacity());

            byteBuffer.put(outputStream.toByteArray());

            byteBuffer.flip();
        } catch (IOException e) {
            // Ignore?
            Timber.w(e, "Caught IOException, ignoring (" + dataSourceNumber + ")");
        } catch (BufferOverflowException e) {
            Timber.w(e, "Caught BufferOverflowException, ignoring (" + dataSourceNumber + ")");
        } finally {
            lock.unlock();
            // Ignore
        }
    }
}
