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

package org.tvheadend.tvhclient.service.htsp.tasks;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import org.tvheadend.tvhclient.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.service.htsp.HtspNotConnectedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles a Subscription on a HTSP Connection
 */
public class Subscriber implements HtspMessage.Listener, Authenticator.Listener {
    private static final String TAG = Subscriber.class.getSimpleName();

    private static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final int INVALID_START_TIME = -1;
    private static final int STATS_INTERVAL = 10000;
    private static final int DEFAULT_TIMESHIFT_PERIOD = 0;

    // Copy of TvInputManager.TIME_SHIFT_INVALID_TIME, available on M+ Only.
    public static final long INVALID_TIMESHIFT_TIME = -9223372036854775808L;

    private static final Set<String> HANDLED_METHODS = new HashSet<>(Arrays.asList(new String[]{
            "subscriptionStart", "subscriptionStatus", "subscriptionStop",
            "queueStatus", "signalStatus", "timeshiftStatus", "muxpkt",
            "subscriptionSkip", "subscriptionSpeed",
            // "subscriptionGrace"
    }));

    private static final AtomicInteger subscriptionCount = new AtomicInteger();

    /**
     * A listener for Subscription events
     */
    public interface Listener {
        void onSubscriptionStart(@NonNull HtspMessage message);
        void onSubscriptionStatus(@NonNull HtspMessage message);
        void onSubscriptionStop(@NonNull HtspMessage message);
        void onSubscriptionSkip(@NonNull HtspMessage message);
        void onSubscriptionSpeed(@NonNull HtspMessage message);
        void onQueueStatus(@NonNull HtspMessage message);
        void onSignalStatus(@NonNull HtspMessage message);
        void onTimeshiftStatus(@NonNull HtspMessage message);
        void onMuxpkt(@NonNull HtspMessage message);
    }

    private final HtspMessage.Dispatcher dispatcher;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final int subscriptionId;

    private Timer timer;
    private HtspMessage queueStatus;
    private HtspMessage signalStatus;
    private HtspMessage timeshiftStatus;

    private long channelId;
    private String profile;
    private int timeshiftPeriod = 0;
    private long startTime = INVALID_START_TIME;

    private boolean isSubscribed = false;

    public Subscriber(@NonNull HtspMessage.Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.subscriptionId = subscriptionCount.incrementAndGet();
    }

    public void addSubscriptionListener(Listener listener) {
        if (listeners.contains(listener)) {
            Log.w(TAG, "Attempted to add duplicate subscription listener");
            return;
        }
        listeners.add(listener);
    }

    public void removeSubscriptionListener(Listener listener) {
        if (!listeners.contains(listener)) {
            Log.w(TAG, "Attempted to remove non existing subscription listener");
            return;
        }
        listeners.remove(listener);
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void subscribe(long channelId) throws HtspNotConnectedException {
        subscribe(channelId, null, DEFAULT_TIMESHIFT_PERIOD);
    }

    public void subscribe(long channelId, String profile) throws HtspNotConnectedException {
        subscribe(channelId, profile, DEFAULT_TIMESHIFT_PERIOD);
    }

    public void subscribe(long channelId, int timeshiftPeriod) throws HtspNotConnectedException {
        subscribe(channelId, null, timeshiftPeriod);
    }

    public void subscribe(long channelId, String profile, int timeshiftPeriod) throws HtspNotConnectedException {
        Log.i(TAG, "Requesting subscription to channel " + this.channelId);

        if (!isSubscribed) {
            dispatcher.addMessageListener(this);
        }

        this.channelId = channelId;
        this.profile = profile;

        HtspMessage subscribeRequest = new HtspMessage();

        subscribeRequest.put("method", "subscribe");
        subscribeRequest.put("subscriptionId", subscriptionId);
        subscribeRequest.put("channelId", channelId);
        subscribeRequest.put("timeshiftPeriod", timeshiftPeriod);

        if (this.profile != null) {
            subscribeRequest.put("profile", this.profile);
        }

        HtspMessage subscribeResponse = dispatcher.sendMessage(subscribeRequest, 5000);

        this.timeshiftPeriod = subscribeResponse.getInteger("timeshiftPeriod", 0);
        Log.i(TAG, "Available timeshift period in seconds: " + this.timeshiftPeriod);

        isSubscribed = true;

        startTimer();
    }

    public void unsubscribe() {
        Log.i(TAG, "Requesting unsubscription from channel " + channelId);

        cancelTimer();

        isSubscribed = false;

        dispatcher.removeMessageListener(this);

        HtspMessage unsubscribeRequest = new HtspMessage();

        unsubscribeRequest.put("method", "unsubscribe");
        unsubscribeRequest.put("subscriptionId", subscriptionId);

        try {
            dispatcher.sendMessage(unsubscribeRequest);
        } catch (HtspNotConnectedException e) {
            // Ignore: If we're not connected, TVHeadend has already unsubscribed us
        }
    }

    public void setSpeed(int speed) {
        Log.i(TAG, "Requesting speed " + speed + " for channel " + channelId);

        HtspMessage subscriptionSpeedRequest = new HtspMessage();

        subscriptionSpeedRequest.put("method", "subscriptionSpeed");
        subscriptionSpeedRequest.put("subscriptionId", subscriptionId);
        subscriptionSpeedRequest.put("speed", speed);

        try {
            dispatcher.sendMessage(subscriptionSpeedRequest);
        } catch (HtspNotConnectedException e) {
            // Ignore: If we're not connected, TVHeadend has already unsubscribed us
        }
    }

    public void pause() {
        setSpeed(0);
    }

    public void resume() {
        setSpeed(100);
    }

    public void skip(long time) {
        Log.i(TAG, "Requesting skip for channel " + channelId);

        HtspMessage subscriptionSkipRequest = new HtspMessage();

        subscriptionSkipRequest.put("method", "subscriptionSkip");
        subscriptionSkipRequest.put("subscriptionId", subscriptionId);
        subscriptionSkipRequest.put("time", time);
        subscriptionSkipRequest.put("absolute", 1);

        try {
            dispatcher.sendMessage(subscriptionSkipRequest);
        } catch (HtspNotConnectedException e) {
            // Ignore: If we're not connected, TVHeadend has already unsubscribed us
        }
    }

    public void live() {
        Log.i(TAG, "Requesting live for channel " + channelId);

        HtspMessage subscriptionLiveRequest = new HtspMessage();

        subscriptionLiveRequest.put("method", "subscriptionLive");
        subscriptionLiveRequest.put("subscriptionId", subscriptionId);

        try {
            dispatcher.sendMessage(subscriptionLiveRequest);
        } catch (HtspNotConnectedException e) {
            // Ignore: If we're not connected, TVHeadend has already unsubscribed us
        }
    }

    public long getTimeshiftOffsetPts() {
        if (timeshiftStatus != null) {
            return timeshiftStatus.getLong("shift") * -1;
        }

        return INVALID_TIMESHIFT_TIME;
    }

    public long getTimeshiftStartPts() {
        if (timeshiftStatus != null) {
            return timeshiftStatus.getLong("start", INVALID_TIMESHIFT_TIME);
        }

        return INVALID_TIMESHIFT_TIME;
    }

    public long getTimeshiftStartTime() {
        long startPts = getTimeshiftStartPts();

        if (startPts == INVALID_TIMESHIFT_TIME || startTime == INVALID_START_TIME) {
            return INVALID_TIMESHIFT_TIME;
        }

        return startTime + startPts;
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    // HtspMessage.Listener Methods
    @Override
    public void onMessage(@NonNull HtspMessage message) {
        final String method = message.getString("method", null);

        if (HANDLED_METHODS.contains(method)) {
            final int subscriptionId = message.getInteger("subscriptionId", INVALID_SUBSCRIPTION_ID);

            if (subscriptionId != this.subscriptionId) {
                // This message relates to a different subscription, don't handle it
                return;
            }

            switch (method) {
                case "subscriptionStart":
                    onSubscriptionStart(message);
                    for (final Listener listener : listeners) {
                        listener.onSubscriptionStart(message);
                    }
                    break;
                case "subscriptionStatus":
                    onSubscriptionStatus(message);
                    for (final Listener listener : listeners) {
                        listener.onSubscriptionStatus(message);
                    }
                    break;
                case "subscriptionStop":
                    onSubscriptionStop(message);
                    for (final Listener listener : listeners) {
                        listener.onSubscriptionStop(message);
                    }
                    break;
                case "subscriptionSkip":
                    for (final Listener listener : listeners) {
                        listener.onSubscriptionSkip(message);
                    }
                    break;
                case "subscriptionSpeed":
                    for (final Listener listener : listeners) {
                        listener.onSubscriptionSpeed(message);
                    }
                    break;
                case "queueStatus":
                    onQueueStatus(message);
                    for (final Listener listener : listeners) {
                        listener.onQueueStatus(message);
                    }
                    break;
                case "signalStatus":
                    onSignalStatus(message);
                    for (final Listener listener : listeners) {
                        listener.onSignalStatus(message);
                    }
                    break;
                case "timeshiftStatus":
                    onTimeshiftStatus(message);
                    for (final Listener listener : listeners) {
                        listener.onTimeshiftStatus(message);
                    }
                    break;
                case "muxpkt":
                    for (final Listener listener : listeners) {
                        listener.onMuxpkt(message);
                    }
                    break;
            }
        }
    }

    // Authenticator.Listener Methods
    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {
        if (isSubscribed && state == Authenticator.State.AUTHENTICATED) {
            Log.w(TAG, "Resubscribing to channel " + channelId);
            try {
                subscribe(channelId, profile, timeshiftPeriod);
            } catch (HtspNotConnectedException e) {
                Log.e(TAG, "Resubscribing to channel failed, not connected");
            }
        }
    }

    // Misc Internal Methods
    private void onSubscriptionStart(@NonNull HtspMessage message) {
        // TODO: -1000 is a total hack, we're running this about 500ms after the actual start time..
        startTime = (System.currentTimeMillis() * 1000) - 1000;
    }

    private void onSubscriptionStatus(@NonNull HtspMessage message) {
        final int subscriptionId = message.getInteger("subscriptionId");
        final String status = message.getString("status", null);
        final String subscriptionError = message.getString("subscriptionError", null);

        if (status != null || subscriptionError != null) {
            StringBuilder builder = new StringBuilder()
                    .append("Subscription Status:")
                    .append(" S: ").append(subscriptionId);

            if (status != null) {
                builder.append(" Status: ").append(status);
            }

            if (subscriptionError != null) {
                builder.append(" Error: ").append(subscriptionError);
            }

            Log.w(TAG, builder.toString());
        }
    }

    private void onSubscriptionStop(@NonNull HtspMessage message) {
        cancelTimer();
    }

    private void onQueueStatus(@NonNull HtspMessage message) {
        queueStatus = message;
    }

    private void onSignalStatus(@NonNull HtspMessage message) {
        signalStatus = message;
    }

    private void onTimeshiftStatus(@NonNull HtspMessage message) {
        timeshiftStatus = message;
    }

    private void startTimer() {
        cancelTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new StatsTimerTask(), STATS_INTERVAL, STATS_INTERVAL);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private class StatsTimerTask extends TimerTask {
        @Override
        public void run() {
            if (queueStatus != null) {
                try {
                    logQueueStatus();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to log queue status", e);
                }
            }

            if (signalStatus != null) {
                try {
                    logSignalStatus();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to log signal status", e);
                }
            }

            if (timeshiftStatus != null) {
                try {
                    logTimeshiftStatus();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to log timeshift status", e);
                }
            }
        }

        private void logQueueStatus() {
            final int subscriptionId = queueStatus.getInteger("subscriptionId");
            final int packets = queueStatus.getInteger("packets");
            final int bytes = queueStatus.getInteger("bytes");
            final int errors = queueStatus.getInteger("errors", 0);
            final long delay = queueStatus.getLong("delay");
            final int bDrops = queueStatus.getInteger("Bdrops");
            final int pDrops = queueStatus.getInteger("Pdrops");
            final int iDrops = queueStatus.getInteger("Idrops");

            StringBuilder builder = new StringBuilder()
                    .append("Queue Status:")
                    .append(" S: ").append(subscriptionId)
                    .append(" P: ").append(packets)
                    .append(" B: ").append(bytes)
                    .append(" E: ").append(errors)
                    .append(" D: ").append(delay)
                    .append(" bD: ").append(bDrops)
                    .append(" pD: ").append(pDrops)
                    .append(" iD: ").append(iDrops);

            Log.i(TAG, builder.toString());
        }

        private void logSignalStatus() {
            final int subscriptionId = signalStatus.getInteger("subscriptionId");
            final String feStatus = signalStatus.getString("feStatus");
            final int feSNR = signalStatus.getInteger("feSNR", -1);
            final int feSignal = signalStatus.getInteger("feSignal", -1);
            final int feBER = signalStatus.getInteger("feBER", -1);
            final int feUNC = signalStatus.getInteger("feUNC", -1);

            StringBuilder builder = new StringBuilder()
                    .append("Signal Status:")
                    .append(" S: ").append(subscriptionId)
                    .append(" feStatus: ").append(feStatus);

            if (feSNR != -1) {
                builder.append(" feSNR: ").append(feSNR);
            }

            if (feSignal != -1) {
                builder.append(" feSignal: ").append(feSignal);
            }

            if (feBER != -1) {
                builder.append(" feBER: ").append(feBER);
            }

            if (feUNC != -1) {
                builder.append(" feUNC: ").append(feUNC);
            }

            Log.i(TAG, builder.toString());
        }

        private void logTimeshiftStatus() {
            final int subscriptionId = timeshiftStatus.getInteger("subscriptionId");
            final int full = timeshiftStatus.getInteger("full");
            final long shift = timeshiftStatus.getLong("shift");
            final long start = timeshiftStatus.getLong("start", -1);
            final long end = timeshiftStatus.getLong("end", -1);

            StringBuilder builder = new StringBuilder()
                    .append("Timeshift Status:")
                    .append(" S: ").append(subscriptionId)
                    .append(" full: ").append(full)
                    .append(" shift: ").append(shift);

            if (start != -1) {
                builder.append(" start: ").append(start);
            }

            if (end != -1) {
                builder.append(" end: ").append(end);
            }

            Log.i(TAG, builder.toString());
        }
    }
}
