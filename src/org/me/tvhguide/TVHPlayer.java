/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.me.tvhguide;

import android.util.Log;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.me.tvhguide.model.Packet;

/**
 *
 * @author john-tornblom
 */
public class TVHPlayer {

    private static final long BUFFER_TIME = 5 * 1000 * 1000; //us
    private static final double MESSURE_TIME = 2 * 1000 * 1000; //us
    private static int videoIndex;
    private static int audioIndex;
    private static boolean buffering;
    private static boolean running;
    private static ArrayList<Packet> buffer;
    private static long duration;
    private static Lock lock;
    private static double networkSpeed;
    private static double relativeDuration;
    private static Date messureStarted;

    static {
        System.loadLibrary("tvhplayer");

        lock = new ReentrantLock();
        buffer = new ArrayList<Packet>();
        buffering = true;
        running = false;
        videoIndex = audioIndex = -1;
        networkSpeed = 0;
        messureStarted = null;
    }

    public static boolean isBuffering() {
        return buffering;
    }

    public static int getVideoIndex() {
        return videoIndex;
    }

    public static int getAudioIndex() {
        return audioIndex;
    }

    public static boolean isRunning() {
        return running;
    }

    public static double getNetworkSpeed() {
        return networkSpeed;
    }

    public static boolean enqueue(Packet packet) {
        try {
            lock.lock();

            if (!running) {
                return false;
            }

            if (packet.stream.index == audioIndex) {
                Date now = new Date();
                if (messureStarted == null || messureStarted.getTime() + (MESSURE_TIME / 1000) < now.getTime()) {
                    messureStarted = new Date();
                    networkSpeed = relativeDuration / MESSURE_TIME;
                    relativeDuration = 0;
                    Log.d("TVHPlayer", "Estimated network speed: " + networkSpeed + "x");
                }

                relativeDuration += packet.duration;
            }

            if (!buffering) {
                boolean isPlaying = play(packet);
                buffering = !isPlaying;
                return isPlaying;
            }

            if (audioIndex < 0 && packet.stream.index != videoIndex && setAudioCodec(packet.stream.type)) {
                audioIndex = packet.stream.index;
            } else if (videoIndex < 0 && packet.stream.index != audioIndex && setVideoCodec(packet.stream.type)) {
                videoIndex = packet.stream.index;
            } else if (packet.stream.index != videoIndex && packet.stream.index != audioIndex) {
                return false;
            }

            buffer.add(packet);

            if (packet.stream.index == audioIndex) {
                duration += packet.duration;
                buffering = duration < BUFFER_TIME;

                int progress = (int) ((100 * duration) / BUFFER_TIME);

                Log.d("TVHPlayer", "Buffering: " + progress + "%");
            }

            if (!buffering) {
                for (Packet p : buffer) {
                    play(p);
                }
                buffer.clear();
                duration = 0;
                System.gc();
            }

            return !buffering;
        } finally {
            lock.unlock();
        }
    }

    public static void startPlayback() {
        lock.lock();
        try {
            running = true;
            start();
        } finally {
            lock.unlock();
        }
    }

    public static void stopPlayback() {
        lock.lock();
        try {
            videoIndex = audioIndex = -1;
            buffering = true;
            buffer.clear();
            running = false;
            networkSpeed = 0;
            messureStarted = null;
            stop();
        } finally {
            lock.unlock();
        }
    }

    private static boolean play(Packet p) {
        boolean isPlaying = !buffering;

        if (p.stream.index == audioIndex) {
            isPlaying = enqueueAudioFrame(p.payload, p.pts, p.dts, p.duration);
        } else if (p.stream.index == videoIndex) {
            enqueueVideoFrame(p.payload, p.pts, p.dts, p.duration);
        }

        return isPlaying;
    }

    public static native void setSurface(Surface surface);

    private static native void start();

    private static native void stop();

    private static native boolean setAudioCodec(String codec);

    private static native boolean setVideoCodec(String codec);

    private static native boolean enqueueAudioFrame(byte[] frame, long pts, long dts, long duration);

    private static native boolean enqueueVideoFrame(byte[] frame, long pts, long dts, long duration);

    public static native int getWidth();

    public static native int getHeight();

    public static native int getAspectDen();

    public static native int getAspectNum();
}
