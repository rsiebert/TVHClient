package org.tvheadend.tvhclient.features.streaming.internal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.Util;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncHandler;
import org.tvheadend.tvhclient.utils.MiscUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

// TODO use service that is called from the menu
// TODO service starts htspsession
// TODO tvheadendplayer needs to get the views
// TODO in tvheadendplayer get playerview and setPlayer(...)

public class HtspPlaybackActivity extends AppCompatActivity implements View.OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener, Player.EventListener {

    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected EpgSyncHandler epgSyncHandler;
    @Inject
    protected AppRepository appRepository;
    private Handler handler;
    private int channelId;
    private int recordingId;

    @BindView(R.id.controls_root)
    protected LinearLayout debugRootView;
    @BindView(R.id.player_view)
    protected PlayerView playerView;

    private SimpleExoPlayer player;
    //private EventLogger eventLogger;
    private TvheadendTrackSelector trackSelector;
    private HtspDataSource.Factory htspSubscriptionDataSourceFactory;
    private HtspDataSource.Factory htspFileInputStreamDataSourceFactory;
    private HtspDataSource dataSource;
    private MediaSource mediaSource;
    private TvheadendExtractorsFactory extractorsFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Timber.d("start");
        setContentView(R.layout.player_overlay_view);
        MiscUtils.setLanguage(this);

        MainApplication.getComponent().inject(this);
        ButterKnife.bind(this);

        HandlerThread handlerThread = new HandlerThread("PlaybackSession Handler Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        playerView = findViewById(R.id.player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.requestFocus();

        if (savedInstanceState != null) {
            channelId = getIntent().getIntExtra("channelId", -1);
            recordingId = getIntent().getIntExtra("recordingId", -1);
        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", -1);
                recordingId = getIntent().getIntExtra("recordingId", -1);
            }
        }
        Timber.d("end");
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
        startPlayback();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        Timber.d("Initializing player");

        trackSelector = new TvheadendTrackSelector(new AdaptiveTrackSelection.Factory(null));
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", false)) {
            trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(this));
        }

        DefaultLoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                Integer.parseInt(sharedPreferences.getString("buffer_playback_ms", "500")),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                C.DEFAULT_BUFFER_SEGMENT_SIZE,
                true);

        player = ExoPlayerFactory.newSimpleInstance(
                new TvheadendRenderersFactory(this), trackSelector, loadControl);
        player.addListener(this);
        playerView.setPlayer(player);
        playerView.setPlaybackPreparer(this);

        // Add the EventLogger
        //eventLogger = new EventLogger(trackSelector);
        //player.addListener(eventLogger);
        //player.addAudioDebugListener(eventLogger);
        //player.addVideoDebugListener(eventLogger);


        Connection connection = appRepository.getConnectionData().getActiveItem();
        ServerStatus serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());
        ServerProfile playbackProfile = appRepository.getServerProfileData().getItemById(serverStatus.getPlaybackServerProfileId());
        Timber.d("Playback profile is " + playbackProfile.getName());

        // Produces DataSource instances through which media data is loaded.
        //htspSubscriptionDataSourceFactory = new HtspSubscriptionDataSource.Factory(this, epgSyncHandler.getConnection(), sharedPreferences.getString("htsp_stream_profile", "htsp"));
        htspSubscriptionDataSourceFactory = new HtspSubscriptionDataSource.Factory(this, epgSyncHandler.getConnection(), playbackProfile.getName());
        htspFileInputStreamDataSourceFactory = new HtspFileInputStreamDataSource.Factory(this, epgSyncHandler.getConnection());

        // Produces Extractor instances for parsing the media data.
        extractorsFactory = new TvheadendExtractorsFactory(this);
    }

    private void stopPlayback() {
        Timber.d("Stopping player");
        player.stop();
        trackSelector.clearSelectionOverrides();
        htspSubscriptionDataSourceFactory.releaseCurrentDataSource();
        htspFileInputStreamDataSourceFactory.releaseCurrentDataSource();
        if (mediaSource != null) {
            mediaSource.releaseSource();
        }
    }

    private void releasePlayer() {
        Timber.d("Releasing player");
        stopPlayback();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void startPlayback() {
        Timber.d("startPlayback");
        stopPlayback();

        // Create the media source
        if (channelId > 0) {
            Uri channelUri = Uri.parse("htsp://channel/" + channelId);
            Timber.d("Playing channel uri " + channelUri);
            mediaSource = new ExtractorMediaSource.Factory(htspSubscriptionDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(channelUri, handler, null);

        } else if (recordingId > 0) {
            Uri recordingUri = Uri.parse("htsp://dvrfile/" + recordingId);
            Timber.d("Playing recording uri " + recordingUri);
            mediaSource = new ExtractorMediaSource.Factory(htspFileInputStreamDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(recordingUri, handler, null);
        }

        // Prepare the media source
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    /*

        @Override
        public void onResume() {
            super.onResume();
            Timber.d("onResume, playing channel id " + channelId);
            handler.removeCallbacks(mPlayRunnable);
            mPlayRunnable = new PlayChannelRunnable(mTvheadendPlayer, channelId);
            handler.post(mPlayRunnable);
        }

        @Override
        public void onPause() {
            super.onPause();
            mTvheadendPlayer.release();
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Timber.d("Session onPlayerError " + error.getMessage());
        }

        @Override
        public void onTracksChanged(List<TvTrackInfo> tracks, SparseArray<String> selectedTracks) {
            Timber.d("Session : onTracksChanged " + tracks.size());

            //notifyTracksChanged(tracks);

            for (int i = 0; i < selectedTracks.size(); i++) {
                final int selectedTrackType = selectedTracks.keyAt(i);
                final String selectedTrackId = selectedTracks.get(selectedTrackType);

                //notifyTrackSelected(selectedTrackType, selectedTrackId);
            }
        }

        public boolean onPlayChannel(int channelId) {
            Timber.d("Start Playback of a Live Channel " + channelId);

            handler.removeCallbacks(mPlayRunnable);
            mPlayRunnable = new PlayChannelRunnable(mTvheadendPlayer, channelId);
            handler.post(mPlayRunnable);

            return true;
        }

        public void onPlayRecording(int recordedProgramId) {
            Timber.d("Start Playback of a Recorded Program " + recordedProgramId);

            handler.removeCallbacks(mPlayRunnable);
            mPlayRunnable = new PlayRecordedProgramRunnable(mTvheadendPlayer, recordedProgramId);
            handler.post(mPlayRunnable);
        }
    */
    @Override
    public void onClick(View view) {
        Timber.d("Clicked");
    }

    @Override
    public void preparePlayback() {
        Timber.d("Prepare Playback");
    }

    @Override
    public void onVisibilityChange(int visibility) {
        Timber.d("onVisibilityChange");
        Timber.d("Player is " + (visibility != View.VISIBLE ? " not " : "") + "visible");
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        Timber.d("onTimelineChanged");
    }

    /*
        private boolean getTrackStatusBoolean(TrackSelection selection, TrackGroup group, int trackIndex) {
            return selection != null
                    && selection.getTrackGroup() == group
                    && selection.indexOf(trackIndex) != C.INDEX_UNSET;
        }
    */
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Timber.d("onTracksChanged");
/*
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        // Process Tracks
        List<TvTrackInfo> tracks = new ArrayList<>();
        SparseArray<String> selectedTracks = new SparseArray<>();

        // Keep track of weather we have a video track available
        boolean hasVideoTrack = false;

        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.length; rendererIndex++) {
            TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            TrackSelection trackSelection = trackSelections.get(rendererIndex);
            if (rendererTrackGroups.length > 0) {
                for (int groupIndex = 0; groupIndex < rendererTrackGroups.length; groupIndex++) {
                    TrackGroup trackGroup = rendererTrackGroups.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        int formatSupport = mappedTrackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex);

                        if (formatSupport == RendererCapabilities.FORMAT_HANDLED) {
                            Format format = trackGroup.getFormat(trackIndex);
                            TvTrackInfo tvTrackInfo = ExoPlayerUtils.buildTvTrackInfo(format);

                            if (tvTrackInfo != null) {
                                tracks.add(tvTrackInfo);

                                Boolean selected = getTrackStatusBoolean(trackSelection, trackGroup, trackIndex);

                                if (selected) {
                                    int trackType = MimeTypes.getTrackType(format.sampleMimeType);

                                    switch (trackType) {
                                        case C.TRACK_TYPE_VIDEO:
                                            hasVideoTrack = true;
                                            selectedTracks.put(TvTrackInfo.TYPE_VIDEO, format.id);
                                            break;
                                        case C.TRACK_TYPE_AUDIO:
                                            selectedTracks.put(TvTrackInfo.TYPE_AUDIO, format.id);
                                            break;
                                        case C.TRACK_TYPE_TEXT:
                                            selectedTracks.put(TvTrackInfo.TYPE_SUBTITLE, format.id);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        */
/*
        if(hasVideoTrack) {
            disableRadioInfoScreen();
        } else {
            enableRadioInfoScreen();
        }
*/
        //mListener.onTracksChanged(tracks, selectedTracks);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Timber.d("onLoadingChanged is loading " + isLoading);
        if (isLoading) {
            // Fetch the current DataSource for later use
            // TODO: Hold a WeakReference to the DataSource instead...
            // TODO: We should know if we're playing a channel or a recording...
            dataSource = htspSubscriptionDataSourceFactory.getCurrentDataSource();
            if (dataSource == null) {
                dataSource = htspFileInputStreamDataSourceFactory.getCurrentDataSource();
            }
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Timber.d("onPlayerStateChanged: " + playbackState);

        switch (playbackState) {
            case Player.STATE_READY:
                Timber.d("Player is ready");
                break;
            case Player.STATE_BUFFERING:
                Timber.d("Video is not available because the TV input stopped the playback temporarily to buffer more data.");
                break;
            case Player.STATE_ENDED:
                Timber.d("A generic reason. Video is not available due to an unspecified error.");
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        Timber.d("onRepeatModeChanged");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Timber.d("onShuffleModeEnabledChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Timber.d("onPlayerError");
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Timber.d("onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Timber.d("onPlaybackParametersChanged");
    }

    @Override
    public void onSeekProcessed() {
        Timber.d("onSeekProcessed");
    }
}
