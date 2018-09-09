package org.tvheadend.tvhclient.features.streaming.external;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.service.EpgSyncService;

import timber.log.Timber;

public class PlayChannelActivity extends BasePlaybackActivity {

    private int channelId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            channelId = getIntent().getIntExtra("channelId", -1);
        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", -1);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("channelId", channelId);
    }

    @Override
    protected void getHttpTicket() {
        Intent intent = new Intent(this, EpgSyncService.class);
        intent.setAction("getTicket");
        intent.putExtra("channelId", channelId);
        startService(intent);
    }

    @Override
    protected void onHttpTicketReceived() {
        Channel channel = appRepository.getChannelData().getItemById(channelId);

        Timber.d("Playing channel from server with url " + serverUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(serverUrl), "video/*");
        intent.putExtra("itemTitle", channel.getName());
        intent.putExtra("title", channel.getName());
        startExternalPlayer(intent);
    }
}
