package org.tvheadend.tvhclient.features.streaming.internal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
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
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.video.VideoListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;
import org.tvheadend.tvhclient.features.streaming.internal.utils.ExoPlayerUtils;
import org.tvheadend.tvhclient.features.streaming.internal.utils.TvhMappings;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static org.tvheadend.tvhclient.features.streaming.internal.HtspDataSource.INVALID_TIMESHIFT_TIME;

// TODO Disable buttons until data source has been loaded and video is playing

public class HtspPlaybackActivity extends AppCompatActivity implements View.OnClickListener, PlaybackPreparer, Player.EventListener, Authenticator.Listener, PlayerControlView.VisibilityListener, VideoListener, MediaSource.SourceInfoRefreshListener {

    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;

    @BindView(R.id.status)
    TextView statusTextView;
    @BindView(R.id.player_root_view)
    protected FrameLayout playerRootView;
    @BindView(R.id.player_view)
    protected PlayerView playerView;
    @BindView(R.id.channel_icon)
    protected ImageView iconImageView;
    @BindView(R.id.channel_name)
    protected TextView iconTextView;
    @BindView(R.id.program_title)
    protected TextView titleTextView;
    @BindView(R.id.program_subtitle)
    protected TextView subtitleTextView;
    @BindView(R.id.next_program_title)
    protected TextView nextTitleTextView;
    @BindView(R.id.progress)
    protected DefaultTimeBar progressBar;
    @BindView(R.id.duration)
    protected TextView durationTextView;
    @BindView(R.id.player_menu)
    protected ImageButton playerMenuImageView;

    @BindView(R.id.player_rewind)
    protected ImageButton rewindImageView;
    @BindView(R.id.player_pause)
    protected ImageButton pauseImageView;
    @BindView(R.id.player_play)
    protected ImageButton playImageView;
    @BindView(R.id.player_forward)
    protected ImageButton forwardImageView;

    private Handler handler;
    private int channelId;
    private int dvrId;
    private SimpleExoPlayer player;
    private TvheadendTrackSelector trackSelector;
    private HtspDataSource.Factory htspSubscriptionDataSourceFactory;
    private HtspDataSource.Factory htspFileInputStreamDataSourceFactory;
    private WeakReference<HtspDataSource> htspDataSource;
    private MediaSource mediaSource;
    private TvheadendExtractorsFactory extractorsFactory;
    private ServerStatus serverStatus;
    private ServerProfile serverProfile;
    private boolean playerIsPaused = false;
    private SimpleHtspConnection simpleHtspConnection;
    private float videoAspectRatio;
    private int videoWidth;
    private int videoHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_overlay_view);
        MiscUtils.setLanguage(this);
        Timber.d("Creating");

        MainApplication.getComponent().inject(this);
        ButterKnife.bind(this);

        HandlerThread handlerThread = new HandlerThread("PlaybackSession Handler Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        if (savedInstanceState != null) {
            channelId = getIntent().getIntExtra("channelId", -1);
            dvrId = getIntent().getIntExtra("dvrId", -1);
            playerIsPaused = getIntent().getBooleanExtra("playerIsPaused", false);
            videoAspectRatio = getIntent().getFloatExtra("videoAspectRatio", 1.0f);
        } else {
            playerIsPaused = true;
            videoAspectRatio = 1.0f;
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", -1);
                dvrId = bundle.getInt("dvrId", -1);
            }
        }

        serverStatus = appRepository.getServerStatusData().getActiveItem();
        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getHtspPlaybackServerProfileId());

        playerView.setControllerVisibilityListener(this);
        playerView.requestFocus();

        updatePlayerButtonStates();
    }

    private void updatePlayerButtonStates() {
        if (playerIsPaused) {
            pauseImageView.setVisibility(View.GONE);
            playImageView.setVisibility(View.VISIBLE);
        } else {
            pauseImageView.setVisibility(View.VISIBLE);
            playImageView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("playerIsPaused", playerIsPaused);
        outState.putFloat("videoAspectRatio", videoAspectRatio);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Timber.d("New intent");
        releasePlayer();
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("Starting");
        Connection connection = appRepository.getConnectionData().getActiveItem();
        simpleHtspConnection = new SimpleHtspConnection(connection);
        simpleHtspConnection.addAuthenticationListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("Resuming");
        initialize();
    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.d("Pausing");
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("Stopping");

        if (simpleHtspConnection != null) {
            simpleHtspConnection.removeAuthenticationListener(this);
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
    }

    private void initialize() {
        if (serverStatus == null) {
            Timber.d("Server status is null");
            statusTextView.setText(R.string.error_starting_playback_no_connection);
        } else if (serverProfile == null) {
            Timber.d("Server profile is null");
            statusTextView.setText(R.string.error_starting_playback_no_profile);
        } else {
            Timber.d("Starting htsp connection service");
            statusTextView.setText(R.string.connecting_to_server);
            // start the connection here so that the authentication callback
            // will not fire before the server profile is checked here
            simpleHtspConnection.start();
        }
    }

    private void showPlaybackInformation(String channelName, String channelIcon, String title, String subtitle, String nextTitle, long duration) {
        Picasso.get()
                .load(UIUtils.getIconUrl(this, channelIcon))
                .into(iconImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        iconTextView.setVisibility(View.INVISIBLE);
                        iconImageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        iconTextView.setText(channelName);
                        iconTextView.setVisibility(!TextUtils.isEmpty(channelName) ? View.VISIBLE : View.GONE);
                        iconImageView.setVisibility(View.INVISIBLE);
                    }
                });

        titleTextView.setText(title);
        subtitleTextView.setVisibility(!TextUtils.isEmpty(subtitle) ? View.VISIBLE : View.GONE);
        subtitleTextView.setText(subtitle);
        nextTitleTextView.setVisibility(!TextUtils.isEmpty(nextTitle) ? View.VISIBLE : View.GONE);
        nextTitleTextView.setText(getString(R.string.next_program, nextTitle));

        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.US);
        durationTextView.setText(sdf.format(duration));
    }

    private void initializePlayer() {
        Timber.d("Initializing player");

        trackSelector = new TvheadendTrackSelector(new AdaptiveTrackSelection.Factory(null));
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", false)) {
            trackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(this));
        }

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        Integer.parseInt(sharedPreferences.getString("buffer_playback_ms", "500")),
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .setTargetBufferBytes(C.DEFAULT_BUFFER_SEGMENT_SIZE)
                .setPrioritizeTimeOverSizeThresholds(true)
                .createDefaultLoadControl();

        player = ExoPlayerFactory.newSimpleInstance(
                new TvheadendRenderersFactory(this), trackSelector, loadControl);
        player.addListener(this);
        player.addVideoListener(this);

        playerView.setPlayer(player);
        playerView.setPlaybackPreparer(this);

        // Produces DataSource instances through which media data is loaded.
        htspSubscriptionDataSourceFactory = new HtspSubscriptionDataSource.Factory(
                this, simpleHtspConnection, serverProfile.getName());
        htspFileInputStreamDataSourceFactory = new HtspFileInputStreamDataSource.Factory(
                this, simpleHtspConnection);

        // Produces Extractor instances for parsing the media data.
        extractorsFactory = new TvheadendExtractorsFactory(this);
    }

    private void stopPlayback() {
        Timber.d("Stopping playback and releasing data and media sources");

        if (player != null) {
            player.stop();
        }
        if (trackSelector != null) {
            trackSelector.buildUponParameters().clearSelectionOverrides();
        }
        if (htspSubscriptionDataSourceFactory != null) {
            htspSubscriptionDataSourceFactory.releaseCurrentDataSource();
        }
        if (htspFileInputStreamDataSourceFactory != null) {
            htspFileInputStreamDataSourceFactory.releaseCurrentDataSource();
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
        Timber.d("Starting playback");

        playerRootView.setVisibility(View.VISIBLE);

        // Create the media source
        if (channelId > 0) {
            Channel channel = appRepository.getChannelData().getItemByIdWithPrograms(channelId, new Date().getTime());
            Program program = appRepository.getProgramData().getItemById(channel.getProgramId());
            long duration = (program != null ? (program.getStop() - new Date().getTime()) : 0);
            showPlaybackInformation(channel.getName(),
                    channel.getIcon(),
                    channel.getProgramTitle(),
                    channel.getProgramSubtitle(),
                    channel.getNextProgramTitle(),
                    duration);

            Uri channelUri = Uri.parse("htsp://channel/" + channelId);
            Timber.d("Channel uri " + channelUri);
            mediaSource = new ExtractorMediaSource.Factory(htspSubscriptionDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(channelUri);

        } else if (dvrId > 0) {
            Recording recording = appRepository.getRecordingData().getItemById(dvrId);
            showPlaybackInformation(recording.getChannelName(),
                    recording.getChannelIcon(),
                    recording.getTitle(),
                    recording.getSubtitle(),
                    null,
                    (recording.getStop() - recording.getStart()));

            Uri recordingUri = Uri.parse("htsp://dvrfile/" + dvrId);
            Timber.d("Recording uri " + recordingUri);
            mediaSource = new ExtractorMediaSource.Factory(htspFileInputStreamDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(recordingUri);
        }

        // Prepare the media source
        if (player != null) {
            Timber.d("Preparing player and starting when ready");
            player.prepare(mediaSource);
            player.setPlayWhenReady(true);
        }
    }

    /*
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
    */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_rewind:
                Timber.d("Rewind clicked");
                //onSeekButtonSelected(Math.max(getTimeshiftCurrentPosition() - (-5000), getTimeshiftStartPosition()));
                break;

            case R.id.player_pause:
                Timber.d("Pause clicked");
                onPauseButtonSelected();
                break;

            case R.id.player_play:
                Timber.d("Play clicked");
                onPlayButtonSelected();
                break;

            case R.id.player_forward:
                Timber.d("Forward clicked");
                //onSeekButtonSelected(Math.max(getTimeshiftCurrentPosition() - 5000, getTimeshiftStartPosition()));
                break;

            case R.id.player_menu:
                onChangeAspectRatioSelected();
                break;
        }
    }

    private void onChangeAspectRatioSelected() {
        String[] aspectRatioNameList = new String[]{
                "5:4 (1.25:1)",
                "4:3 (1.3:1)",
                "16:9 (1.7:1)"
        };
        List<Float> aspectRatioValueList = Arrays.asList(1.25f, 1.3f, 1.7f);

        int selectedAspectRatioListIndex = 2;
        for (int i = 0; i < aspectRatioValueList.size(); i++) {
            if (aspectRatioValueList.get(i) == videoAspectRatio) {
                selectedAspectRatioListIndex = i;
            }
        }

        new MaterialDialog.Builder(this)
                .title("Select the video aspect ratio")
                .items(aspectRatioNameList)
                .itemsCallbackSingleChoice(selectedAspectRatioListIndex, (dialog, dialogView, which, text) -> {
                    Timber.d("Selected aspect ratio index is " + which + ", value " + aspectRatioNameList[which]);
                    videoAspectRatio = aspectRatioValueList.get(which);

                    Timber.d("Old video dimensions are w:" + videoWidth + ", h:" + videoHeight);
                    videoWidth = (int) (videoHeight * videoAspectRatio);
                    Timber.d("New video dimensions are w:" + videoWidth + ", h:" + videoHeight);

                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int screenWidth = size.x;
                    int screenHeight = size.y;
                    Timber.d("Screen dimensions are w:" + screenWidth + ", h:" + screenHeight);

                    if (videoWidth < screenWidth) {
                        Timber.d("Video width is less that the screen width");
                        float factor = screenWidth / videoWidth;

                        videoWidth = (int) (videoWidth * factor);
                        videoHeight = (int) (videoHeight * factor);
                        Timber.d("New scaled video dimensions are w:" + videoWidth + ", h:" + videoHeight);
                    }
                    /*
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Timber.d("Device is in landscape");
                    } else {
                        Timber.d("Device is in portrait");
                    }
                    */
                    Timber.d("Setting layout params");
                    ViewGroup.LayoutParams layoutParams = playerView.getVideoSurfaceView().getLayoutParams();
                    layoutParams.width = videoWidth;
                    layoutParams.height = videoHeight;
                    playerView.getVideoSurfaceView().setLayoutParams(layoutParams);

                    return true;
                })
                .show();
    }


    public void onPlayButtonSelected() {
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        if (playerIsPaused) {
            onResumeButtonSelected();
        }
    }

    public void onResumeButtonSelected() {
        if (htspDataSource == null) {
            return;
        }
        HtspDataSource dataSource = htspDataSource.get();
        if (dataSource != null) {
            Timber.d("Resuming HtspDataSource");
            dataSource.resume();
                /*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PlaybackParams normalParams = new PlaybackParams();
                    normalParams.setSpeed(1);
                    onTimeShiftSetPlaybackParams(normalParams);
                }
                */
        } else {
            Timber.w("Unable to resume, no HtspDataSource available");
        }
    }

    public void onPauseButtonSelected() {
        if (htspDataSource == null) {
            return;
        }
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        HtspDataSource dataSource = htspDataSource.get();
        if (dataSource != null) {
            Timber.d("Pausing HtspDataSource");
            dataSource.pause();
        } else {
            Timber.w("Unable to pause, no HtspDataSource available");
        }
    }

    @Override
    public void preparePlayback() {
        Timber.d("Prepare Playback");
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
        Timber.d("Loading has changed to " + isLoading);

        if (isLoading) {
            // Fetch the current DataSource for later use
            if (channelId > 0) {
                htspDataSource = new WeakReference<>(htspSubscriptionDataSourceFactory.getCurrentDataSource());
            } else if (dvrId > 0) {
                htspDataSource = new WeakReference<>(htspFileInputStreamDataSourceFactory.getCurrentDataSource());
            }

            Timber.d("Adding click listeners to the player buttons");
            rewindImageView.setOnClickListener(this);
            pauseImageView.setOnClickListener(this);
            playImageView.setOnClickListener(this);
            forwardImageView.setOnClickListener(this);
            playerMenuImageView.setOnClickListener(this);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                Timber.d("Player is idle");
                break;
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

        if (playWhenReady && playbackState == Player.STATE_READY) {
            Timber.d("Media is playing");
            playerIsPaused = false;

        } else if (playWhenReady) {
            Timber.d("Player might be idle (plays after prepare()), " +
                    "buffering (plays when data available) " +
                    "or ended (plays when seek away from end)");

        } else {
            Timber.d("Player is paused in any state");
            playerIsPaused = true;
        }

        updatePlayerButtonStates();
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // NOP
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // NOP
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Timber.d("Player error occurred", error);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Timber.d("Could seek within media source, reason " + reason);
        switch (reason) {
            case Player.DISCONTINUITY_REASON_PERIOD_TRANSITION:
                Timber.d("Automatic playback transition from one period in the timeline to the next.");
                break;
            case Player.DISCONTINUITY_REASON_SEEK:
                Timber.d("Seek within the current period or to another period.");
                break;
            case Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                Timber.d("Seek adjustment due to being unable to seek to the requested position or because the seek was permitted to be inexact.");
                break;
            case Player.DISCONTINUITY_REASON_AD_INSERTION:
                Timber.d("Discontinuity to or from an ad within one period in the timeline.");
                break;
            case Player.DISCONTINUITY_REASON_INTERNAL:
                Timber.d("Discontinuity introduced internally by the source.");
                break;
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // NOP
    }

    @Override
    public void onSeekProcessed() {
        // NOP
    }

    public void onSeekButtonSelected(long timeMs) {
        Timber.d("Seeking to " + timeMs);

        HtspDataSource dataSource = htspDataSource.get();
        if (dataSource != null) {
            Timber.d("Timeshift start time " + dataSource.getTimeshiftStartTime());
            Timber.d("Timeshift start pts " + dataSource.getTimeshiftStartPts());

            long seekPts = (timeMs * 1000) - dataSource.getTimeshiftStartTime();
            seekPts = Math.max(seekPts, dataSource.getTimeshiftStartPts()) / 1000;
            Timber.d("Seeking to PTS: " + seekPts);

            player.seekTo(seekPts);
        } else {
            Timber.w("Unable to seek, no HtspDataSource available");
        }
    }

    public long getTimeshiftStartPosition() {
        HtspDataSource dataSource = this.htspDataSource.get();
        if (dataSource != null) {
            long startTime = dataSource.getTimeshiftStartTime();
            if (startTime != INVALID_TIMESHIFT_TIME) {
                // For live content
                return startTime / 1000;
            } else {
                // For recorded content
                return 0;
            }
        } else {
            Timber.w("Unable to get timeshift start position, no HtspDataSource available");
        }

        return INVALID_TIMESHIFT_TIME;
    }

    public long getTimeshiftCurrentPosition() {
        HtspDataSource dataSource = this.htspDataSource.get();
        if (dataSource != null) {
            long offset = dataSource.getTimeshiftOffsetPts();
            if (offset != INVALID_TIMESHIFT_TIME) {
                // For live content
                return System.currentTimeMillis() + (offset / 1000);
            } else {
                // For recorded content
                return player.getCurrentPosition();
            }
        } else {
            Timber.w("Unable to get current timeshift position, no HtspDataSource available");
        }
        return INVALID_TIMESHIFT_TIME;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
        Timber.d("Setting playback speed to " + params.getSpeed());

        HtspDataSource dataSource = htspDataSource.get();
        if (dataSource != null) {

            float speed = params.getSpeed();
            if (params.getSpeed() == 1) {
                player.setVolume(1.0f);
            } else {
                player.setVolume(0f);
            }

            if (speed > 0) {
                // Forward Playback. Convert from TIF speed format, over to TVH and ExoPlayer formats
                int tvhSpeed = TvhMappings.androidSpeedToTvhSpeed(speed);
                float exoSpeed = ExoPlayerUtils.androidSpeedToExoPlayerSpeed(speed);
                dataSource.setSpeed(tvhSpeed);
                player.setPlaybackParameters(new PlaybackParameters(exoSpeed, 1));

            } else {
                // Reverse Playback
                Snackbar.make(getCurrentFocus(), "Rewind unsupported", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {
        Timber.d("Authentication changed to " + state);
        if (state == Authenticator.State.AUTHENTICATED) {
            runOnUiThread(() -> {
                initializePlayer();
                startPlayback();
            });
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {
        Timber.d("Visibility changed to " + visibility);
        if (visibility != View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= 19) {

                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                                // Set the content to appear under the system bars so that the
                                // content doesn't resize when the system bars hide and show.
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                // Hide the nav bar and status bar
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Timber.d("Video size changed to width " + width + ", height " + height + ", pixel aspect ratio " + pixelWidthHeightRatio);
        videoWidth = width;
        videoHeight = height;
        videoAspectRatio = ((float) width / (float) height);
    }

    @Override
    public void onRenderedFirstFrame() {
        // NOP
    }

    @Override
    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, @Nullable Object manifest) {
        Timber.d("Source info has been refreshed");
    }
}
