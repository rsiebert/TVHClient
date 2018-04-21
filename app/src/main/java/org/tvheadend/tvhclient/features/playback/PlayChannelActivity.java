package org.tvheadend.tvhclient.features.playback;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.remote.EpgSyncService;

import timber.log.Timber;

public class PlayChannelActivity extends BasePlayActivity {

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
    protected void onHttpTicketReceived(String path, String ticket) {
        Timber.d("Playing channel from server");

        Channel channel = new ChannelAndProgramRepository(this).getChannelByIdSync(channelId);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(getPlayerUrl(path, ticket)), getPlayerMimeType());
        intent.putExtra("itemTitle", channel.getName());
        intent.putExtra("title", channel.getName());
        startExternalPlayer(intent);
    }
}
