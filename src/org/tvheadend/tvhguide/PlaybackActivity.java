/*
 *  Copyright (C) 2013 Robert Siebert
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
package org.tvheadend.tvhguide;

import java.util.Date;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.HttpTicket;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlaybackActivity extends Activity implements HTSListener {

    private TVHVideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getStringExtra("title");
        setTitle(title);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.player_layout);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        final TextView clock = (TextView) findViewById(R.id.pl_clock);
        final LinearLayout headerOverlay = (LinearLayout) findViewById(R.id.pl_header);
        final LinearLayout middleOverlay = (LinearLayout) findViewById(R.id.pl_middle);
        final LinearLayout footerOverlay = (LinearLayout) findViewById(R.id.pl_footer);

        videoView = (TVHVideoView) findViewById(R.id.pl_video);
        videoView.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer arg0) {
                middleOverlay.setVisibility(LinearLayout.GONE);
            }
        });

        videoView.setOnErrorListener(new OnErrorListener() {

            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                finish();
                return true;
            }
        });

        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.channel_list_widget, null, false);
//        final ChannelListViewWrapper w = new ChannelListViewWrapper(v);
        footerOverlay.addView(v);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.pl_frame);
        frameLayout.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if (headerOverlay.getVisibility() == LinearLayout.VISIBLE) {
                    headerOverlay.setVisibility(LinearLayout.INVISIBLE);
                    footerOverlay.setVisibility(LinearLayout.INVISIBLE);
                } else {
//                    w.repaint(channel);
                    clock.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
                    clock.setText(DateFormat.getTimeFormat(clock.getContext()).format(new Date()));
                    headerOverlay.setVisibility(LinearLayout.VISIBLE);
                    footerOverlay.setVisibility(LinearLayout.VISIBLE);
                }
            }
        });

        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_GET_TICKET);
        intent.putExtras(getIntent().getExtras());
        startService(intent);
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
        
        String host = getIntent().getStringExtra("serverHostPref");
        Integer port = getIntent().getIntExtra("httpPortPref", 9981);
        Integer resolution = getIntent().getIntExtra("resolutionPref", 288);
        Boolean transcode = getIntent().getBooleanExtra("transcodePref", false);
        String container = getIntent().getStringExtra("containerPref");
        String acodec = getIntent().getStringExtra("acodecPref");
        String vcodec = getIntent().getStringExtra("vcodecPref");
        String scodec = getIntent().getStringExtra("scodecPref");
        
        String url = "http://" + host + ":" + port + path;
        url += "?ticket=" + ticket;
        url += "&mux=" + container;
        if (transcode) {
            url += "&transcode=1";
            url += "&resolution=" + resolution;
            url += "&acodec=" + acodec;
            url += "&vcodec=" + vcodec;
            url += "&scodec=" + scodec;
        }
        Log.i("PlaybackActivity", "Playing URL " + url);

        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
        videoView.start();
        videoView.setAspectRatio(16, 9);

        TextView codecInfo = (TextView) findViewById(R.id.pl_codec);

        container = valueToName(R.array.pref_container_list,
                R.array.pref_container_list_display, container);

        String c = container;
        if (transcode) {
            c += " (";
            c += acodec + ", ";
            c += vcodec + "@" + resolution;
            c += ")";
        }

        codecInfo.setGravity(Gravity.CENTER_HORIZONTAL);
        codecInfo.setText(c);
    }

    private String valueToName(int valueRresouce, int nameResource, String val) {

        String[] names = getResources().getStringArray(nameResource);
        String[] values = getResources().getStringArray(valueRresouce);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(val)) {
                return names[i];
            }
        }
        return "";
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