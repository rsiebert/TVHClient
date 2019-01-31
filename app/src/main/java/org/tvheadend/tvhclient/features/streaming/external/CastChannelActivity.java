package org.tvheadend.tvhclient.features.streaming.external;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.utils.MiscUtils;

import timber.log.Timber;

public class CastChannelActivity extends BasePlaybackActivity {

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

        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getCastingServerProfileId());
    }

    @Override
    protected boolean requireHostnameToAddressConversion() {
        return true;
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

        CastSession castSession = MiscUtils.getCastSession(this);
        if (castSession == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.no_cast_session));
            return;
        }

        Channel channel = appRepository.getChannelData().getItemById(channelId);

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, channel.getProgramTitle());
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, channel.getProgramSubtitle());

        if (!TextUtils.isEmpty(channel.getIcon())) {
            String iconUrl;
            if (channel.getIcon().startsWith("http")) {
                iconUrl = channel.getIcon();
            } else {
                iconUrl = baseUrl + "/" + channel.getIcon();
            }
            Timber.d("Channel icon url: " + iconUrl);
            movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
            movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon
        }

        Timber.d("Channel casting url: " + serverUrl);
        MediaInfo mediaInfo = new MediaInfo.Builder(serverUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(0)
                .build();

        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText("Could not get the remote media client from the cast session.");
            return;
        }

        remoteMediaClient.registerCallback(new CastRemoteMediaClientCallback(remoteMediaClient));

        MediaLoadOptions mediaLoadOptions = new MediaLoadOptions.Builder()
                .setAutoplay(true)
                .setPlayPosition(0)
                .build();

        remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                Timber.d("Status updated");
                Intent intent = new Intent(CastChannelActivity.this, ExpandedControlsActivity.class);
                startActivity(intent);
                remoteMediaClient.unregisterCallback(this);
                finish();
            }

            @Override
            public void onSendingRemoteMediaRequest() {
                Timber.d("Sending remote media request");
            }
        });

        remoteMediaClient.load(mediaInfo, mediaLoadOptions);
    }
}
