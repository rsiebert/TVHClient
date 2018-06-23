package org.tvheadend.tvhclient.features.playback;

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
        serverProfile = appRepository.getServerProfileData()
                .getItemById(serverStatus.getPlaybackServerProfileId());
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

        Channel channel = appRepository.getChannelData().getItemById(channelId);
        String url = "http://" + baseUrl + path + "?ticket=" + ticket + "&profile=" + serverProfile.getName();

        Timber.d("Playing channel from server with url " + url);

        // Mime types depending on the selected profile are available under the following link.
        // https://github.com/tvheadend/tvheadend/blob/66d6161c563181e5a572337ab3509a835c5a57e2/src/webui/static/tv.js#L56
        // Currently it is not possible to determine which mime type to use from the profile name.
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "video/x-matroska");
        intent.putExtra("itemTitle", channel.getName());
        intent.putExtra("title", channel.getName());
        startExternalPlayer(intent);
    }
}
