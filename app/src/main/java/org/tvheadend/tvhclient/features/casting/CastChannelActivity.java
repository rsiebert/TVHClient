package org.tvheadend.tvhclient.features.casting;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;

import timber.log.Timber;

public class CastChannelActivity extends BaseCastingActivity {

    private Channel channel;
    private ServerProfile castingProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            channel = new ChannelAndProgramRepository(this).getChannelByIdSync(getIntent().getIntExtra("channelId", -1));
            if (channel == null) {
                Timber.d("No channel was provided");
                finish();
            }
        }

        castingProfile = configRepository.getCastingServerProfileById(serverStatus.getCastingServerProfileId());
        if (castingProfile == null) {
            Timber.d("No casting profile defined");
            finish();
        }
    }

    @Override
    protected void onHttpTicketReceived(String path, String ticket) {
        String baseUrl = getBaseUrl(connection) + serverStatus.getWebroot();
        String iconUrl = baseUrl + "/" + channel.getIcon();

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, channel.getProgramTitle());
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, channel.getProgramSubtitle());
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        String url = baseUrl + "/stream/channelnumber/" + channel.getNumber() + "?profile=" + castingProfile.getName();
        Timber.d("Trying to cast channel with url: " + url);

        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(0)
                .build();

        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClient.load(mediaInfo, true, 0);
        finish();
    }
}
