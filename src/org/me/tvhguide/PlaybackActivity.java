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

import android.widget.LinearLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import org.me.tvhguide.htsp.HTSService;
import android.content.Intent;
import org.me.tvhguide.model.Subscription;
import org.me.tvhguide.htsp.HTSListener;
import android.app.Activity;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Packet;

/**
 *
 * @author john-tornblom
 */
public class PlaybackActivity extends Activity implements HTSListener {

    private TextView bDrops;
    private TextView iDrops;
    private TextView pDrops;
    private TextView delay;
    private TextView queSize;
    private TextView packetCount;
    private TextView status;
    private int seconds;

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


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout l = new LinearLayout(this);
        TextView textView = new TextView(this);
        textView.setText("Status: ");
        l.addView(textView);
        status = new TextView(this);
        l.addView(status);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Dropped B-Frames: ");
        l.addView(textView);
        bDrops = new TextView(this);
        l.addView(bDrops);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Dropped I-Frames: ");
        l.addView(textView);
        iDrops = new TextView(this);
        l.addView(iDrops);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Dropped P-Frames: ");
        l.addView(textView);
        pDrops = new TextView(this);
        l.addView(pDrops);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Runtime: ");
        l.addView(textView);
        delay = new TextView(this);
        l.addView(delay);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Que size: ");
        l.addView(textView);
        queSize = new TextView(this);
        l.addView(queSize);
        layout.addView(l);

        l = new LinearLayout(this);
        textView = new TextView(this);
        textView.setText("Packets in que: ");
        l.addView(textView);
        packetCount = new TextView(this);
        l.addView(packetCount);
        layout.addView(l);


        Button btn = new Button(this);
        btn.setText("Stop");

        btn.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
                intent.setAction(HTSService.ACTION_UNSUBSCRIBE);
                intent.putExtra("subscriptionId", (long) 1);
                startService(intent);
                finish();
            }
        });

        layout.addView(btn);

        ScrollView view = new ScrollView(this);
        view.addView(layout);

        setContentView(view);

        Intent intent = new Intent(PlaybackActivity.this, HTSService.class);
        intent.setAction(HTSService.ACTION_SUBSCRIBE);
        intent.putExtra("subscriptionId", (long) 1);
        intent.putExtra("channelId", channel.id);
        
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

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_SUBSCRIPTION_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Subscription subscription = (Subscription) obj;
                    status.setText(subscription.status);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_SUBSCRIPTION_UPDATE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Subscription subscription = (Subscription) obj;
                    bDrops.setText(Long.toString(subscription.droppedBFrames));
                    iDrops.setText(Long.toString(subscription.droppedIFrames));
                    pDrops.setText(Long.toString(subscription.droppedPFrames));
                    delay.setText(Long.toString(seconds++));
                    queSize.setText(Long.toString(subscription.queSize));
                    packetCount.setText(Long.toString(subscription.packetCount));
                    status.setText(subscription.status);
                }
            });
        } else if (subscription != null && action.equals(TVHGuideApplication.ACTION_PLAYBACK_PACKET)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Packet p = (Packet)obj;
                }
            });
        }
    }
}
