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

import android.content.Intent;
import org.me.tvhguide.htsp.HTSListener;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.HttpTicket;

/**
 *
 * @author john-tornblom
 */
public class ExternalPlaybackActivity extends Activity implements HTSListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(ExternalPlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_GET_TICKET);

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

    private DisplayMetrics getMaxDisplayMetrics(double confHeight) {
        DisplayMetrics d = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(d);

        double maxWidth = d.widthPixels;
        double maxHeight = d.heightPixels;

        if (maxHeight > maxWidth) {
            double tmp = maxHeight;
            maxHeight = maxWidth;
            maxWidth = tmp;
        }

        if (confHeight < maxHeight) {
            maxWidth *= (confHeight / maxHeight);
            maxHeight = confHeight;
        }

        d.widthPixels = (int) maxWidth;
        d.heightPixels = (int) maxHeight;

        return d;
    }

    private void startPlayback(String path, String ticket) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String host = prefs.getString("serverHostPref", "localhost");
        Integer port = Integer.parseInt(prefs.getString("httpPortPref", "9981"));
        Boolean transcode = prefs.getBoolean("transcodePref", true);
        Integer maxHeight = Integer.parseInt(prefs.getString("resolutionPref", "288"));
        DisplayMetrics d = getMaxDisplayMetrics(maxHeight);

        String url = "http://" + host + ":" + port + path;
        url += "?ticket=" + ticket;
        url += "&t=" + (transcode ? 1 : 0);
        url += "&w=" + d.widthPixels;
        url += "&h=" + d.heightPixels;

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "video/x-matroska");


        this.runOnUiThread(new Runnable() {

            public void run() {
                startActivity(intent);
                finish();
            }
        });
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_TICKET_ADD)) {
            HttpTicket t = (HttpTicket) obj;
            startPlayback(t.path, t.ticket);
        }
    }
}
