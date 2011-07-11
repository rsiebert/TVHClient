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

import org.me.tvhguide.htsp.HTSService;
import android.content.Intent;
import org.me.tvhguide.model.Subscription;
import org.me.tvhguide.htsp.HTSListener;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Packet;
import org.me.tvhguide.model.Stream;

/**
 *
 * @author john-tornblom
 */
public class PlaybackActivity extends Activity implements HTSListener {

    private long subId;
    private long channelId;
    private static long nextSubId = 1;
    private SurfaceHolder surfaceHolder;
    private TextView playerStatus;
    private boolean wantPlayback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        Channel channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null) {
            finish();
            return;
        }

        setTitle(channel.name);
        channelId = channel.id;

        setContentView(R.layout.player_layout);

        SurfaceView surface = (SurfaceView) findViewById(R.id.player_surface);
        surface.setMinimumHeight(100);
        surface.setMinimumWidth(100);
        surface.setKeepScreenOn(true);

        surfaceHolder = surface.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        surfaceHolder.addCallback(surfaceCallback);

        subId = nextSubId++;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        playerStatus = (TextView) findViewById(R.id.player_status);
    }
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            TVHPlayer.setSurface(holder.getSurface());
        }

        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyGuardManager.newKeyguardLock(getClass().getName());
        mLock.disableKeyguard();

        wantPlayback = true;

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
        startPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();

        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyGuardManager.newKeyguardLock(getClass().getName());
        mLock.reenableKeyguard();

        wantPlayback = false;

        stopPlayback();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    private void stopPlayback() {
        long id = TVHPlayer.stopPlayback();
        if (id > 0) {
            Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
            intent.setAction(HTSService.ACTION_UNSUBSCRIBE);
            intent.putExtra("subscriptionId", id);
            startService(intent);
        }
    }

    private void startPlayback() {
        stopPlayback();

        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_SUBSCRIBE);
        intent.putExtra("subscriptionId", subId);
        intent.putExtra("channelId", channelId);
        startService(intent);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_SUBSCRIPTION_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Subscription subscription = (Subscription) obj;
                    playerStatus.setText(subscription.status);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_SUBSCRIPTION_UPDATE)) {
            if (!TVHPlayer.isPlaying() && wantPlayback) {
                final Subscription subscription = (Subscription) obj;
                TVHPlayer.startPlayback(subscription);

                runOnUiThread(new Runnable() {

                    public void run() {
                        playerStatus.setText(subscription.status);
                        if (subscription.status == null || subscription.status.length() == 0) {
                            playerStatus.setVisibility(TextView.INVISIBLE);
                        } else {
                            playerStatus.setVisibility(TextView.VISIBLE);
                        }
                    }
                });

            }
            runOnUiThread(new Runnable() {

                public void run() {
                    Subscription subscription = (Subscription) obj;
                    for (Stream st : subscription.streams) {
                        if (st.index == TVHPlayer.getVideoStreamIndex()) {

                            surfaceHolder.setFixedSize(st.width, st.height);
                            break;
                        }
                    }
                }
            });

            runOnUiThread(new Runnable() {

                public void run() {/*
                    Subscription subscription = (Subscription) obj;
                    bDrops.setText(Long.toString(subscription.droppedBFrames));
                    iDrops.setText(Long.toString(subscription.droppedIFrames));
                    pDrops.setText(Long.toString(subscription.droppedPFrames));
                    delay.setText(Long.toString(seconds++));
                    queSize.setText(Long.toString(subscription.queSize));
                    packetCount.setText(Long.toString(subscription.packetCount));
                    status.setText(subscription.status);*/

                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PLAYBACK_PACKET)) {
            Packet p = (Packet) obj;
            TVHPlayer.enqueuePacket(p);
        }
    }
}
