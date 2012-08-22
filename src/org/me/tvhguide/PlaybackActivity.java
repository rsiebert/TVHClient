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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import org.me.tvhguide.htsp.HTSListener;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.HttpTicket;
import org.me.tvhguide.model.Stream;

/**
 *
 * @author john-tornblom
 */
public class PlaybackActivity extends Activity implements HTSListener {

    private TVHVideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("externalPref", false)) {
            Intent intent = new Intent(this, ExternalPlaybackActivity.class);
            intent.putExtras(this.getIntent().getExtras());
            startActivity(intent);
            finish();
            return;
        }

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        final Channel channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null) {
            finish();
            return;
        }

        setTitle(channel.name);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.player_layout);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        videoView = (TVHVideoView) findViewById(R.id.player_video_view);
        videoView.setOnErrorListener(new OnErrorListener() {

            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                finish();
                return false;
            }
        });

        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_GET_TICKET);

        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.channel_list_widget, null, false);
        final ChannelListViewWrapper w = new ChannelListViewWrapper(v);

        final LinearLayout overlay = (LinearLayout) findViewById(R.id.player_overlay);
        overlay.setVisibility(LinearLayout.INVISIBLE);
        overlay.addView(v);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.player_frame);
        frameLayout.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (overlay.getVisibility() == LinearLayout.VISIBLE) {
                    overlay.setVisibility(LinearLayout.INVISIBLE);
                } else {
                    overlay.setVisibility(LinearLayout.VISIBLE);
                    w.repaint(channel);
                }
            }
        });

        intent.putExtras(getIntent().getExtras());
        this.startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    private void startPlayback(String path, String ticket) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String host = prefs.getString("serverHostPref", "localhost");
        Integer port = Integer.parseInt(prefs.getString("httpPortPref", "9981"));
        Integer resolution = Integer.parseInt(prefs.getString("resolutionPref", "288"));
        Boolean transcode = prefs.getBoolean("transcodePref", true);
        String container = prefs.getString("containerPref", "matroska");
        String acodec = prefs.getString("acodecPref", Stream.STREAM_TYPE_AAC);
        String vcodec = prefs.getString("vcodecPref", Stream.STREAM_TYPE_H264);
        
        String url = "http://" + host + ":" + port + path;
        url += "?ticket=" + ticket;
        url += "&mux=" + container;
        if (transcode) {
            url += "&transcode=1";
            url += "&resolution=" + resolution;
            url += "&acodec=" + acodec;
            url += "&vcodec=" + vcodec;
        }

        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
        videoView.start();
        videoView.setAspectRatio(16, 9);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_TICKET_ADD)) {

            this.runOnUiThread(new Runnable() {

                public void run() {
                    HttpTicket t = (HttpTicket) obj;
                    startPlayback(t.path, t.ticket);
                }
            });

        }
    }
}