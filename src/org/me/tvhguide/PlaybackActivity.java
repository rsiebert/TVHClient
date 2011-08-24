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

import android.view.View;
import org.me.tvhguide.htsp.HTSService;
import android.content.Intent;
import org.me.tvhguide.model.Subscription;
import org.me.tvhguide.htsp.HTSListener;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Packet;
import org.me.tvhguide.model.Programme;
import org.me.tvhguide.model.Stream;

/**
 *
 * @author john-tornblom
 */
public class PlaybackActivity extends Activity implements HTSListener {

    private long subId;
    private long channelId;
    private SurfaceHolder surfaceHolder;
    private View overlay;
    private TextView playerStatus;
    private TextView playerQueue;
    private TextView playerDrops;

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
        subId = 1;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.player_layout);

        SurfaceView surface = (SurfaceView) findViewById(R.id.player_surface);
        surface.setMinimumHeight(100);
        surface.setMinimumWidth(100);
        surface.setKeepScreenOn(true);
        surface.setOnLongClickListener(new OnLongClickListener() {

            public boolean onLongClick(View arg0) {
                if (overlay.getVisibility() == LinearLayout.VISIBLE) {

                    overlay.setVisibility(LinearLayout.INVISIBLE);
                } else {
                    overlay.setVisibility(LinearLayout.VISIBLE);
                }

                return true;
            }
        });

        surfaceHolder = surface.getHolder();
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        surfaceHolder.addCallback(surfaceCallback);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Programme p = channel.epg.iterator().next();
        if(p != null) {
            TextView view = (TextView) findViewById(R.id.player_title);
            view.setText(p.title);
            view = (TextView) findViewById(R.id.player_desc);
            view.setText(p.description);
        }
        
        playerStatus = (TextView) findViewById(R.id.player_status);
        playerQueue = (TextView) findViewById(R.id.player_queue);
        playerDrops = (TextView) findViewById(R.id.player_drops);
        overlay =  findViewById(R.id.player_details);
        overlay.getBackground().setAlpha(127);
    }
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
            new Thread(new Runnable() {

                public void run() {
                    TVHPlayer.setSurface(holder.getSurface());
                }
            }).start();
        }

        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            new Thread(new Runnable() {

                public void run() {
                    TVHPlayer.setSurface(null);
                }
            }).start();
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

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
        startPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopPlayback();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    private void stopPlayback() {
        TVHPlayer.stopPlayback();
        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_UNSUBSCRIBE);
        intent.putExtra("subscriptionId", subId);
        startService(intent);
    }

    private void startPlayback() {
        stopPlayback();

        int maxWidth = getWindowManager().getDefaultDisplay().getWidth();
        int maxHeight = getWindowManager().getDefaultDisplay().getHeight();
        if (maxHeight > maxWidth) {
            maxWidth = maxHeight;
            maxHeight = getWindowManager().getDefaultDisplay().getWidth();
        }

        TVHPlayer.startPlayback();

        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_SUBSCRIBE);
        intent.putExtra("subscriptionId", subId);
        intent.putExtra("channelId", channelId);
        intent.putExtra("channels", 2);
        intent.putExtra("maxWidth", maxWidth);
        intent.putExtra("maxHeight", maxHeight);

        startService(intent);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_SUBSCRIPTION_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Subscription subscription = (Subscription) obj;
                    if (subscription.status != null && subscription.status.length() > 0) {
                        playerStatus.setText("Status: " + subscription.status);
                    } else if(TVHPlayer.isBuffering()){
                        playerStatus.setText("Status: Buffering");
                    } else {
                        playerStatus.setText("Status: OK");
                    }

                    playerQueue.setText("Server queue size: " + Long.toString(subscription.packetCount));
                    long droppedFrames = subscription.droppedBFrames
                            + subscription.droppedPFrames
                            + subscription.droppedIFrames;
                    playerDrops.setText("Dropped frames: " + Long.toString(droppedFrames));

                    for (Stream st : subscription.streams) {
                        if (st.index == TVHPlayer.getVideoIndex()) {
                            surfaceHolder.setFixedSize(st.width, st.height);
                            break;
                        }
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PLAYBACK_PACKET)) {
            Packet p = (Packet) obj;
            TVHPlayer.enqueue(p);
        }
    }
}
