package org.tvheadend.tvhclient.features.casting;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;

import timber.log.Timber;

public class CastRecordingActivity extends BaseCastingActivity {

    private Recording recording;
    private ServerProfile castingProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            recording = appRepository.getRecordingData().getItemById(getIntent().getIntExtra("dvrId", -1));
            if (recording == null) {
                Timber.d("No recording was provided");
                finish();
            }
        }

        castingProfile = appRepository.getServerProfileData().getItemById(serverStatus.getCastingServerProfileId());
        if (castingProfile == null) {
            Timber.d("No playback profile defined");
            finish();
        }
    }

    @Override
    protected void onHttpTicketReceived(String path, String ticket) {
        String baseUrl = getBaseUrl();
        String iconUrl = baseUrl + "/" + recording.getChannelIcon();

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, recording.getTitle());
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, recording.getSubtitle());
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        String url = baseUrl + "/dvrfile/" + recording.getId() + "?profile=" + castingProfile.getName();
        Timber.d("Trying to cast recording with url: " + url);

        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(recording.getStop() - recording.getStart())
                .build();

        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClient.load(mediaInfo, true, 0);
        finish();
    }
}
