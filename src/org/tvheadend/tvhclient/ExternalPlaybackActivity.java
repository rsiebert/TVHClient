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
package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ExternalPlaybackActivity extends Activity implements HTSListener {

    private Context context;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        Intent intent = new Intent(ExternalPlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_GET_TICKET);
        intent.putExtras(getIntent().getExtras());
        this.startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
    }

    /**
     * When the first ticket from the HTSService has been received the URL is
     * created and then passed to the external media player.
     * 
     * @param path
     * @param ticket
     */
    private void startPlayback(String path, String ticket) {
        String host = getIntent().getStringExtra("serverHostPref");
        Integer port = getIntent().getIntExtra("httpPortPref", 9981);
        Integer resolution = getIntent().getIntExtra("resolutionPref", 288);
        Boolean transcode = getIntent().getBooleanExtra("transcodePref", false);
        String container = getIntent().getStringExtra("containerPref");
        String acodec = getIntent().getStringExtra("acodecPref");
        String vcodec = getIntent().getStringExtra("vcodecPref");
        String scodec = getIntent().getStringExtra("scodecPref");
        String mime = "application/octet-stream";

        // Set the correct MIME type. For 'pass' we assume MPEG-TS
        if ("mpegps".equals(container)) {
            mime = "video/mp2p";
        } else if ("mpegts".equals(container)) {
            mime = "video/mp4";
        } else if ("matroska".equals(container)) {
            mime = "video/x-matroska";
        } else if ("pass".equals(container)) {
            mime = "video/mp2t";
        }

        // Create the URL for the external media player that is required to get
        // the stream from the server
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

        final Intent playbackIntent = new Intent(Intent.ACTION_VIEW);
        playbackIntent.setDataAndType(Uri.parse(url), mime);
        Log.d("TVHGuide", "Playing URL " + url);

        // Start playing the video now in the UI thread
        this.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    startActivity(playbackIntent);
                } catch (Throwable t) {
                    Log.e("TVHGuide", "Can't execute external media player", t);

                    // Show a confirmation dialog before deleting the recording
                    new AlertDialog.Builder(context)
                    .setTitle(R.string.no_media_player)
                    .setMessage(R.string.show_play_store)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                startActivity(installIntent);
                            } catch (Throwable t2) {
                                Log.e("TVHGuide", "Can't query market", t2);
                            } finally {
                                finish();
                            }
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
                }
            }
        });
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_TICKET_ADD)) {
            HttpTicket t = (HttpTicket) obj;
            startPlayback(t.path, t.ticket);
        }
    }
}
