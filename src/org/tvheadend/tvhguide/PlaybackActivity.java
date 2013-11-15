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
import android.view.Gravity;
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
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);

        // Set the title (name of the channel or the recording)
        String title = getIntent().getStringExtra("title");
        setTitle(title);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.player_layout);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        final TextView clock = (TextView) findViewById(R.id.pl_clock);
        final LinearLayout header = (LinearLayout) findViewById(R.id.pl_header);
        final LinearLayout middle = (LinearLayout) findViewById(R.id.pl_middle);

        videoView = (TVHVideoView) findViewById(R.id.pl_video);
        videoView.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                middle.setVisibility(LinearLayout.GONE);
            }
        });

        videoView.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                finish();
                return true;
            }
        });

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.pl_frame);
        frameLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // If the header is visible hide it otherwise
                // show it and display the clock and the title
                if (header.getVisibility() == LinearLayout.VISIBLE) {
                    header.setVisibility(LinearLayout.INVISIBLE);
                }
                else {
                    clock.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
                    clock.setText(DateFormat.getTimeFormat(clock.getContext()).format(new Date()));
                    header.setVisibility(LinearLayout.VISIBLE);
                }
            }
        });

        // Get the ticket to play the stream
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

        // Get the values that was passed to this
        // activity by the program selection activity
        String host = getIntent().getStringExtra("serverHostPref");
        Integer port = getIntent().getIntExtra("httpPortPref", 9981);
        Integer resolution = getIntent().getIntExtra("resolutionPref", 288);
        Boolean transcode = getIntent().getBooleanExtra("transcodePref", false);
        String container = getIntent().getStringExtra("containerPref");
        String acodec = getIntent().getStringExtra("acodecPref");
        String vcodec = getIntent().getStringExtra("vcodecPref");
        String scodec = getIntent().getStringExtra("scodecPref");

        // Create the URL that is used to get the stream from the server
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

        // Set the details so the view can play the stream
        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
        videoView.start();
        videoView.setAspectRatio(16, 9);

        // Convert the container information like mpegts
        // to a better readable format like MPEG-TS
        container = valueToName(R.array.pref_container_list, R.array.pref_container_list_display, container);

        // Add additional information to the
        // string if we transcode the stream
        String c = container;
        if (transcode) {
            c += " (";
            c += acodec + ", ";
            c += vcodec + "@" + resolution;
            c += ")";
        }

        // Get the codec widget so we can show the used codec information
        TextView codecInfo = (TextView) findViewById(R.id.pl_codec);
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