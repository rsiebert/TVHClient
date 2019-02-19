package org.tvheadend.tvhclient.ui.features.streaming.internal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Display;
import android.view.SurfaceView;
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
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.video.VideoListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.HtspConnection;
import org.tvheadend.tvhclient.data.service.HtspConnectionStateListener;
import org.tvheadend.tvhclient.ui.features.streaming.internal.utils.ExoPlayerUtils;
import org.tvheadend.tvhclient.ui.features.streaming.internal.utils.TrackSelectionHelper;
import org.tvheadend.tvhclient.ui.features.streaming.internal.utils.TvhMappings;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

// TODO Disable buttons until data source has been loaded and video is playing
// TODO Timeshift support
// TODO select speed

public class HtspPlaybackActivity extends AppCompatActivity implements View.OnClickListener, PlaybackPreparer, Player.EventListener, PlayerControlView.VisibilityListener, VideoListener, HtspConnectionStateListener {

    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;

    @BindView(R.id.exo_player_frame)
    protected FrameLayout playerMainFrame;
    @BindView(R.id.exo_player_surface_view)
    protected SurfaceView playerSurfaceView;
    @BindView(R.id.status)
    protected TextView statusTextView;
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
    @BindView(R.id.elapsed_time)
    protected TextView elapsedTimeTextView;
    @BindView(R.id.remaining_time)
    protected TextView remainingTimeTextView;
    @BindView(R.id.player_menu)
    protected ImageButton playerMenuImageView;
    @BindView(R.id.player_menu_aspect_ratio)
    protected ImageButton playerMenuAspectRatioImageView;

    @BindView(R.id.player_rewind)
    protected ImageButton rewindImageView;
    @BindView(R.id.player_pause)
    protected ImageButton pauseImageView;
    @BindView(R.id.player_play)
    protected ImageButton playImageView;
    @BindView(R.id.player_forward)
    protected ImageButton forwardImageView;

    private int channelId;
    private int dvrId;
    private Channel channel;
    private Recording recording;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;

    // Copy of TvInputManager.TIME_SHIFT_INVALID_TIME, available on M+ Only.
    public static final long INVALID_TIMESHIFT_TIME = -9223372036854775808L;

    private HtspSubscriptionDataSource.Factory htspSubscriptionDataSourceFactory;
    private HtspFileInputStreamDataSource.Factory htspFileInputStreamDataSourceFactory;
    private MediaSource mediaSource;
    private TvheadendExtractorsFactory extractorsFactory;
    private ServerStatus serverStatus;
    private ServerProfile serverProfile;
    private boolean playerIsPaused = false;
    private int videoWidth;
    private int videoHeight;
    private float videoAspectRatio;
    private int selectedAspectRatioListIndex;
    private List<Float> aspectRatioValueList;
    private String[] aspectRatioNameList;

    private long currentTime = new Date().getTime();
    private Runnable currentTimeUpdateTask;
    private final Handler currentTimeUpdateHandler = new Handler();
    private TrackSelectionHelper trackSelectionHelper;

    private HtspDataSourceInterface dataSource;
    private HtspConnection htspConnection;
    private ScheduledExecutorService execService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_overlay_view);
        MiscUtils.setLanguage(this);
        Timber.d("Creating");

        MainApplication.getComponent().inject(this);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            channelId = getIntent().getIntExtra("channelId", -1);
            dvrId = getIntent().getIntExtra("dvrId", -1);
            playerIsPaused = getIntent().getBooleanExtra("playerIsPaused", false);
            videoAspectRatio = getIntent().getFloatExtra("videoAspectRatio", 1.0f);
            selectedAspectRatioListIndex = getIntent().getIntExtra("selectedAspectRatioListIndex", 2);
        } else {
            playerIsPaused = true;
            videoAspectRatio = 1.0f;
            selectedAspectRatioListIndex = 2;
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                channelId = bundle.getInt("channelId", -1);
                dvrId = bundle.getInt("dvrId", -1);
            }
        }

        execService = Executors.newScheduledThreadPool(10);

        if (channelId > 0) {
            channel = appRepository.getChannelData().getItemByIdWithPrograms(channelId, new Date().getTime());
        }
        if (dvrId > 0) {
            recording = appRepository.getRecordingData().getItemById(dvrId);
        }

        aspectRatioNameList = new String[]{
                "5:4 (1.25:1)",
                "4:3 (1.3:1)",
                "16:9 (1.7:1)",
                "16:10 (1.6:1)"
        };
        aspectRatioValueList = Arrays.asList(1.25f, 1.333f, 1.778f, 1.6f);

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
        outState.putInt("selectedAspectRatioListIndex", selectedAspectRatioListIndex);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Timber.d("New intent");
        releasePlayer();
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("Resuming");

        if (serverStatus == null) {
            Timber.d("Server status is null");
            statusTextView.setText(R.string.error_starting_playback_no_connection);
        } else if (serverProfile == null) {
            Timber.d("Server profile is null");
            statusTextView.setText(R.string.error_starting_playback_no_profile);
        } else if (channelId > 0 && channel == null) {
            statusTextView.setText(R.string.error_starting_playback_no_channel);
        } else if (dvrId > 0 && recording == null) {
            statusTextView.setText(R.string.error_starting_playback_no_recording);
        } else {
            Timber.d("Starting htsp connection service");
            statusTextView.setText(R.string.connecting_to_server);

            htspConnection = new HtspConnection(this, null);
            execService.execute(() -> {
                htspConnection.openConnection();
                htspConnection.authenticate();
            });
        }
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
        execService.shutdown();
        if (htspConnection != null) {
            htspConnection.closeConnection();
            htspConnection = null;
        }
    }

    private void showPlaybackInformation(String channelName, String channelIcon, String title, String subtitle, String nextTitle, long start, long stop) {
        // Show the channel icons. Otherwise show the channel name only
        if (!TextUtils.isEmpty(channelIcon)) {
            Picasso.get()
                    .load(MiscUtils.getIconUrl(this, channelIcon))
                    .into(iconImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            iconTextView.setVisibility(View.GONE);
                            iconImageView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        } else {
            iconTextView.setText(channelName);
            iconTextView.setVisibility(View.VISIBLE);
            iconImageView.setVisibility(View.GONE);
        }

        titleTextView.setText(title);
        subtitleTextView.setVisibility(!TextUtils.isEmpty(subtitle) ? View.VISIBLE : View.GONE);
        subtitleTextView.setText(subtitle);
        nextTitleTextView.setVisibility(!TextUtils.isEmpty(nextTitle) ? View.VISIBLE : View.GONE);
        nextTitleTextView.setText(getString(R.string.next_program, nextTitle));

        elapsedTimeTextView.setVisibility(View.INVISIBLE);
        remainingTimeTextView.setVisibility(View.INVISIBLE);

        // Initiate a timer that will update the elapsed and
        // remaining time every second when the media is playing
        currentTimeUpdateTask = () -> {
            elapsedTimeTextView.setVisibility(View.VISIBLE);
            remainingTimeTextView.setVisibility(View.VISIBLE);

            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.US);
            remainingTimeTextView.setText(sdf.format(stop - currentTime));
            elapsedTimeTextView.setText(sdf.format(currentTime - start));

            // Add a second to the time where the program or recording has been started
            currentTime += 1000;

            currentTimeUpdateHandler.postDelayed(currentTimeUpdateTask, 1000);
        };
    }

    private void initializePlayer() {
        Timber.d("Initializing player");

        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(null));
        TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter());
        trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);

        if (sharedPreferences.getBoolean("audio_tunneling_enabled",
                getResources().getBoolean(R.bool.pref_default_audio_tunneling_enabled))) {
            trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(this));
        }

        //noinspection ConstantConditions
        int bufferTime = Integer.parseInt(sharedPreferences.getString("buffer_playback_ms", getResources().getString(R.string.pref_default_buffer_playback_ms)));

        DefaultLoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                bufferTime,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                C.DEFAULT_BUFFER_SEGMENT_SIZE,
                true);

        player = ExoPlayerFactory.newSimpleInstance(
                new TvheadendRenderersFactory(this), trackSelector, loadControl);
        player.addListener(this);
        player.addVideoListener(this);
        player.setVideoSurfaceView(playerSurfaceView);

        playerView.setPlayer(player);
        playerView.setPlaybackPreparer(this);

        Timber.d("Creating data source factories");
        // Produces DataSource instances through which media data is loaded.
        htspSubscriptionDataSourceFactory = new HtspSubscriptionDataSource.Factory(
                this, htspConnection, serverProfile.getName());
        htspFileInputStreamDataSourceFactory = new HtspFileInputStreamDataSource.Factory(
                this, htspConnection);

        // Produces Extractor instances for parsing the media data.
        extractorsFactory = new TvheadendExtractorsFactory(this);
    }

    private void stopPlayback() {
        Timber.d("Stopping playback and releasing data and media sources");

        if (player != null) {
            player.stop();
        }
        if (trackSelector != null) {
            trackSelector.clearSelectionOverrides();
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

        // Create the media source
        if (channelId > 0) {
            Program program = appRepository.getProgramData().getItemById(channel.getProgramId());
            showPlaybackInformation(channel.getName(),
                    channel.getIcon(),
                    channel.getProgramTitle(),
                    channel.getProgramSubtitle(),
                    channel.getNextProgramTitle(),
                    (program != null ? program.getStart() : 0),
                    (program != null ? program.getStop() : 0));

            Uri channelUri = Uri.parse("htsp://channel/" + channelId);
            Timber.d("Channel uri " + channelUri);

            mediaSource = new ExtractorMediaSource.Factory(htspSubscriptionDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(channelUri);

        } else if (dvrId > 0) {
            showPlaybackInformation(recording.getChannelName(),
                    recording.getChannelIcon(),
                    recording.getTitle(),
                    recording.getSubtitle(),
                    null,
                    recording.getStart(),
                    recording.getStop());

            Uri recordingUri = Uri.parse("htsp://dvrfile/" + dvrId);
            Timber.d("Recording uri " + recordingUri);
            mediaSource = new ExtractorMediaSource.Factory(htspFileInputStreamDataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(recordingUri);
        }

        // Prepare the media source
        if (player != null && mediaSource != null) {
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

            case R.id.player_menu_aspect_ratio:
                onChangeAspectRatioSelected();
                break;

            case R.id.player_menu:
                onPlayerMenuSelected(view);
                break;
        }
    }

    private void onPlayerMenuSelected(View view) {
        Timber.d("Player menu selected");

        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.player_popup_menu, popupMenu.getMenu());

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            for (int i = 0; i < mappedTrackInfo.length; i++) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                if (trackGroups.length != 0) {
                    switch (player.getRendererType(i)) {
                        case C.TRACK_TYPE_AUDIO:
                            Timber.d("Track renderer type for index " + i + " is audio, showing audio menu");
                            popupMenu.getMenu().findItem(R.id.menu_audio).setVisible(true);
                            break;
                        case C.TRACK_TYPE_VIDEO:
                            Timber.d("Track renderer type for index " + i + " is video");
                            break;
                        case C.TRACK_TYPE_TEXT:
                            Timber.d("Track renderer type for index " + i + " is text, showing subtitle menu");
                            popupMenu.getMenu().findItem(R.id.menu_subtitle).setVisible(true);
                            break;
                    }
                }
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_audio:
                        trackSelectionHelper.showSelectionDialog(this, "Audio", mappedTrackInfo, C.TRACK_TYPE_AUDIO);
                        return true;
                    case R.id.menu_subtitle:
                        trackSelectionHelper.showSelectionDialog(this, "Subtitles", mappedTrackInfo, C.TRACK_TYPE_TEXT);
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        }
    }

    private void onChangeAspectRatioSelected() {
        Timber.d("Change aspect ratio menu selected");
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
                    selectedAspectRatioListIndex = which;
                    changeAspectRatio();
                    return true;
                })
                .show();
    }

    private void onPlayButtonSelected() {
        Timber.d("Play button selected");
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        if (playerIsPaused) {
            onResumeButtonSelected();
        }
    }

    private void onResumeButtonSelected() {
        Timber.d("Resume button selected");
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

    private void onPauseButtonSelected() {
        Timber.d("Pause button selected");
        if (dataSource == null) {
            return;
        }
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        Timber.d("Pausing HtspDataSource");
        dataSource.pause();
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
            if (channelId > 0 && htspSubscriptionDataSourceFactory != null) {
                dataSource = htspSubscriptionDataSourceFactory.getCurrentDataSource();
            } else if (dvrId > 0 && htspFileInputStreamDataSourceFactory != null) {
                dataSource = htspFileInputStreamDataSourceFactory.getCurrentDataSource();
            }

            Timber.d("Adding click listeners to the player buttons");
            rewindImageView.setOnClickListener(this);
            pauseImageView.setOnClickListener(this);
            playImageView.setOnClickListener(this);
            forwardImageView.setOnClickListener(this);
            playerMenuImageView.setOnClickListener(this);
            playerMenuAspectRatioImageView.setOnClickListener(this);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                Timber.d("Player is idle");
                statusTextView.setVisibility(View.GONE);
                playerSurfaceView.setVisibility(View.VISIBLE);
                break;

            case Player.STATE_READY:
                Timber.d("Player is ready");
                statusTextView.setVisibility(View.GONE);
                playerSurfaceView.setVisibility(View.VISIBLE);
                break;

            case Player.STATE_BUFFERING:
                Timber.d("Video is not available because the TV input stopped the playback temporarily to buffer more data.");
                playerSurfaceView.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                statusTextView.setText(R.string.player_is_loading_mode_data);
                break;

            case Player.STATE_ENDED:
                Timber.d("A generic reason. Video is not available due to an unspecified error.");
                statusTextView.setVisibility(View.GONE);
                break;
        }

        if (playWhenReady && playbackState == Player.STATE_READY) {
            Timber.d("Media is playing");
            playerIsPaused = false;
            currentTimeUpdateHandler.post(currentTimeUpdateTask);

        } else if (playWhenReady) {
            Timber.d("Player might be idle (plays after prepare()), " +
                    "buffering (plays when data available) " +
                    "or ended (plays when seek away from end)");

        } else {
            Timber.d("Player is paused in any state");
            playerIsPaused = true;
            currentTimeUpdateHandler.removeCallbacks(currentTimeUpdateTask);
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

        //HtspDataSource dataSource = htspDataSource.get();
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
        //HtspDataSource dataSource = this.htspDataSource.get();
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
        //HtspDataSource dataSource = this.htspDataSource.get();
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

        //HtspDataSource dataSource = htspDataSource.get();
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
                SnackbarUtils.sendSnackbarMessage(this, "Rewind not supported");
            }
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
        changeAspectRatio();
    }

    private void changeAspectRatio() {
        Timber.d("Original video dimensions are " + videoWidth + ":" + videoHeight);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        int newVideoWidth = videoWidth;
        int newVideoHeight = videoHeight;
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (newVideoWidth != screenWidth) {
                newVideoWidth = screenWidth;
            }
            newVideoHeight = (int) ((float) newVideoWidth / videoAspectRatio);
            Timber.d("New portrait video dimensions are " + newVideoWidth + ":" + newVideoHeight);

        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (newVideoHeight != screenHeight) {
                newVideoHeight = screenHeight;
            }
            newVideoWidth = (int) ((float) newVideoHeight * videoAspectRatio);
            Timber.d("New landscape video dimensions are " + newVideoWidth + ":" + newVideoHeight);
        }

        ViewGroup.LayoutParams layoutParams = playerMainFrame.getLayoutParams();
        layoutParams.width = newVideoWidth;
        layoutParams.height = newVideoHeight;
        playerMainFrame.setLayoutParams(layoutParams);
        playerMainFrame.requestLayout();
    }

    @Override
    public void onRenderedFirstFrame() {
        // NOP
    }

    @Override
    public void onAuthenticationStateChange(@NonNull HtspConnection.AuthenticationState state) {
        switch (state) {
            case FAILED:
            case FAILED_BAD_CREDENTIALS:
                statusTextView.setText(R.string.authentication_failed);
                break;
            case AUTHENTICATED:
                Timber.d("Initializing and starting player");
                runOnUiThread(() -> {
                    initializePlayer();
                    startPlayback();
                });
                break;
        }
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.ConnectionState state) {
        switch(state) {
            case FAILED:
            case FAILED_CONNECTING_TO_SERVER:
            case FAILED_EXCEPTION_OPENING_SOCKET:
            case FAILED_UNRESOLVED_ADDRESS:
            case FAILED_INTERRUPTED:
                statusTextView.setText(R.string.connection_failed);
                break;
            case CONNECTING:
                statusTextView.setText(R.string.connecting_to_server);
                break;
        }
    }
}
