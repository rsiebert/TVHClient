package org.tvheadend.tvhclient.features.streaming.external;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.service.EpgSyncService;

import timber.log.Timber;

public class CastRecordingActivity extends BasePlaybackActivity {

    private int dvrId;
    private CastSession castSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            dvrId = getIntent().getIntExtra("dvrId", -1);
        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                dvrId = bundle.getInt("dvrId", -1);
            }
        }
        CastContext castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();
        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getCastingServerProfileId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (castSession == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.no_cast_session));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dvrId", dvrId);
    }

    @Override
    protected void getHttpTicket() {
        Intent intent = new Intent(this, EpgSyncService.class);
        intent.setAction("getTicket");
        intent.putExtra("dvrId", dvrId);
        startService(intent);
    }

    @Override
    protected void onHttpTicketReceived() {

        Recording recording = appRepository.getRecordingData().getItemById(dvrId);

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, recording.getTitle());
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, recording.getSubtitle());

        if (!TextUtils.isEmpty(recording.getChannelIcon())) {
            String iconUrl;
            if (recording.getChannelIcon().startsWith("http")) {
                iconUrl = recording.getChannelIcon();
            } else {
                iconUrl = baseUrl + "/" + recording.getChannelIcon();
            }
            Timber.d("Recording channel icon url: " + iconUrl);
            movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
            movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon
        }

        Timber.d("Recording casting url: " + serverUrl);
        MediaInfo mediaInfo = new MediaInfo.Builder(serverUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(recording.getStop() - recording.getStart())
                .build();

        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClient.registerCallback(new CastRemoteMediaClientCallback(remoteMediaClient));

        MediaLoadOptions mediaLoadOptions = new MediaLoadOptions.Builder()
                .setAutoplay(true)
                .setPlayPosition(0)
                .build();

        remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                Timber.d("Status updated");
                Intent intent = new Intent(CastRecordingActivity.this, ExpandedControlsActivity.class);
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
