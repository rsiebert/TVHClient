/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.tvhguide;

import android.view.Surface;
import org.me.tvhguide.model.Packet;
import org.me.tvhguide.model.Stream;
import org.me.tvhguide.model.Subscription;

/**
 *
 * @author john
 */
public class TVHPlayer {

    private static long subscriptionId;
    private static int audioStream;
    private static int videoStream;
    private static Surface surface;

    static {
        System.loadLibrary("tvhplayer");
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
                surface.setSize(st.width, st.height);
            }
        }

        if (audioStream != 0 || videoStream != 0) {
            subscriptionId = s.id;
        }
    }

    public static boolean enqueuePacket(Packet p) {
        if (p.subscription.id == subscriptionId && p.stream.index == audioStream) {
            enqueueAudioFrame(p.payload);
            return true;
        } else if (p.subscription.id == subscriptionId && p.stream.index == videoStream) {
            enqueueVideoFrame(p.payload, p.pts, p.dts, p.duration);
            return true;
        }

        return false;
    }

    public static void setVideoSurface(Surface s) {
        TVHPlayer.surface = s;
        setSurface(surface);
    }
    private static native void setSurface(Surface surface);

    private static native boolean setAudioCodec(String codec);

    private static native boolean setVideoCodec(String codec);

    private static native void enqueueAudioFrame(byte[] frame);

    private static native void enqueueVideoFrame(byte[] frame, long pts, long dts, long duration);
}
