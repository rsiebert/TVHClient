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
import java.util.Iterator;

import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.HttpTicket;
import org.tvheadend.tvhguide.model.Program;
import org.tvheadend.tvhguide.model.Recording;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlaybackActivity extends ActionBarActivity implements HTSListener {

    private Channel ch = null;
    private Recording rec = null;
    private ActionBar actionBar = null;
    private TVHVideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_layout);
        
        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        
        // Set the title (name of the channel or the recording)        
        actionBar.setTitle(getIntent().getStringExtra("title"));

        // Get the channel which holds the program
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        ch = app.getChannel(getIntent().getLongExtra("channelId", 0));
        rec = app.getRecording(getIntent().getLongExtra("dvrId", 0));

        // Contains additional information about the played program or recording
        final LinearLayout header = (LinearLayout) findViewById(R.id.player_header);
        TextView title = (TextView) findViewById(R.id.title);
        ImageView state = (ImageView) findViewById(R.id.state);
        TextView summary = (TextView) findViewById(R.id.summary);
        final TextView date = (TextView) findViewById(R.id.date);
        final TextView time = (TextView) findViewById(R.id.time);
        final TextView duration = (TextView) findViewById(R.id.duration);
        final TextView desc = (TextView) findViewById(R.id.description);
        final TextView seriesInfo = (TextView) findViewById(R.id.series_info);

        // Get the first program from the channel
        if (ch != null && ch.epg != null) {
            Log.i("PlaybackActivity", "Got channel and epg");
            Iterator<Program> it = ch.epg.iterator();
            Program p = null;
            if (it.hasNext())
                p = it.next();
            
            if (p != null) {
                Log.i("PlaybackActivity", "Got program from channel");
                title.setText(p.title);
                Utils.setState(state, p.recording);
                Utils.setDate(date, p.start);
                Utils.setTime(time, p.start, p.stop);
                Utils.setDuration(duration, p.start, p.stop);
                Utils.setDescription(null, summary, p.summary);
                Utils.setDescription(null, desc, p.description);
                Utils.setSeriesInfo(null, seriesInfo, p.seriesInfo);
            }
        }
        else if (rec != null) {
            title.setText(rec.title);
            Utils.setState(state, rec);
            Utils.setDate(date, rec.start);
            Utils.setTime(time, rec.start, rec.stop);
            Utils.setDuration(duration, rec.start, rec.stop);
            Utils.setDescription(null, summary, rec.summary);
            Utils.setDescription(null, desc, rec.description);
            seriesInfo.setVisibility(View.GONE);
        }

        videoView = (TVHVideoView) findViewById(R.id.player_video);
        videoView.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                Log.i("PlaybackActivity", "Video player ready");
                // Hide the layout with the progress bar
                final LinearLayout middle = (LinearLayout) findViewById(R.id.player_middle);
                middle.setVisibility(LinearLayout.GONE);
            }
        });

        videoView.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i("PlaybackActivity", "Error playing stream");
                finish();
                return true;
            }
        });

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.player_frame);
        frameLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // If the header is visible hide it otherwise
                // show it and display the clock and the title
                if (actionBar.isShowing()) {
                    header.setVisibility(LinearLayout.INVISIBLE);
                    actionBar.hide();
                }
                else {
                    header.setVisibility(LinearLayout.VISIBLE);
                    actionBar.setSubtitle(DateFormat.getTimeFormat(actionBar.getThemedContext()).format(new Date()));
                    actionBar.show();
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
        TextView codecInfo = (TextView) findViewById(R.id.player_codec);
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