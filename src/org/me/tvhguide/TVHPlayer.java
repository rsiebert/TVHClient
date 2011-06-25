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

import android.view.Surface;
import org.me.tvhguide.model.Packet;
import org.me.tvhguide.model.Stream;
import org.me.tvhguide.model.Subscription;

/**
 *
 * @author john-tornblom
 */
public class TVHPlayer {

    private static long subscriptionId;
    private static int audioStream;
    private static int videoStream;

    static {
        System.loadLibrary("tvhplayer");
    }

    public static int getVideoStreamIndex() {
        return videoStream;
    }

    public static long stop() {
        long ret = subscriptionId;
        subscriptionId = audioStream = videoStream = 0;
        return ret;
    }

    public static boolean isPlaying() {
        return subscriptionId != 0;
    }

    public static void play(Subscription s) {
        if (subscriptionId != 0) {
            return;
        }

        for (Stream st : s.streams) {
            if (audioStream == 0 && setAudioCodec(st.type)) {
                audioStream = st.index;
            } else if (videoStream == 0 && setVideoCodec(st.type)) {
                videoStream = st.index;
            }
        }

        if (audioStream != 0 || videoStream != 0) {
            subscriptionId = s.id;
        }
    }

    public static boolean enqueuePacket(Packet p) {
        if (p.subscription.id == subscriptionId && p.stream.index == audioStream) {
            enqueueAudioFrame(p.payload, p.pts, p.dts, p.duration);
            return true;
        } else if (p.subscription.id == subscriptionId && p.stream.index == videoStream) {
            enqueueVideoFrame(p.payload, p.pts, p.dts, p.duration);
            return true;
        }

        return false;
    }
    
    public static native void setSurface(Surface surface);

    private static native boolean setAudioCodec(String codec);

    private static native boolean setVideoCodec(String codec);

    private static native void enqueueAudioFrame(byte[] frame, long pts, long dts, long duration);

    private static native void enqueueVideoFrame(byte[] frame, long pts, long dts, long duration);
}
