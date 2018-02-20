package org.tvheadend.tvhclient.data.remote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.local.Logger;
import org.tvheadend.tvhclient.data.model.HttpTicket;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;
import org.tvheadend.tvhclient.data.repository.ServerStatusRepository;
import org.tvheadend.tvhclient.service.EpgSyncService;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PlayActivity extends Activity implements OnRequestPermissionsResultCallback {

    private final static String TAG = PlayActivity.class.getSimpleName();

    // Defines what action shall be done
    private final int ACTION_PLAY = 1;
    private final int ACTION_CAST = 2;

    private TVHClientApplication app;
    private int action;

    private Channel ch;
    private Recording rec;
    private String baseUrl;
    private String username;
    private String password;
    private String address;
    private int streamingPort;
    private String title = "";
    private Logger logger;
    private CastContext castContext;
    private CastSession castSession;
    private ServerStatus serverStatus;
    private RecordingRepository repository;
    private ConnectionRepository connectionRepostory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        MiscUtils.setLanguage(this);

        app = TVHClientApplication.getInstance();
        logger = Logger.getInstance();

        serverStatus = new ServerStatusRepository(this).loadServerStatusSync();
        
        // If a play intent was sent no action is given, so default to play
        action = getIntent().getIntExtra(Constants.BUNDLE_ACTION, ACTION_PLAY);

        // Check that a valid channel or recording was specified
        connectionRepostory = new ConnectionRepository(this);
        repository = new RecordingRepository(this);
        ch = repository.getChannelByIdSync(getIntent().getIntExtra("channelId", 0));
        rec = repository.getRecordingByIdSync(getIntent().getIntExtra("dvrId", 0));

        // Get the title from either the channel or recording
        if (ch != null) {
            title = ch.getChannelName();
        } else if (rec != null) {
            title = rec.getTitle();
        } else {
            logger.log(TAG, "onCreate: No channel or recording provided, exiting");
            return;
        }

        //castContext = CastContext.getSharedInstance(this);
        // If the cast menu button is connected then assume playing means casting
        //if (VideoCastManager.getInstance().isConnected()) {
        //    action = ACTION_CAST;
        //}
        castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();
        if (castSession != null) {
            action = ACTION_CAST;
        }
    }

    /**
     *
     */
    private void initAction() {
        logger.log(TAG, "initAction() called");

        // Create the url with the credentials and the host and  
        // port configuration. This one is fixed for all actions
        Connection conn = connectionRepostory.getActiveConnectionSync();
        if (conn != null) {
            username = conn.getUsername();
            password = conn.getPassword();
            address = conn.getHostname();
            streamingPort = conn.getStreamingPort();
        }

        String encodedUsername = "";
        String encodedPassword = "";
        try {
            if (conn != null) {
                if (!username.isEmpty()) {
                    encodedUsername = URLEncoder.encode(username, "UTF-8");
                }
                if (!password.isEmpty()) {
                    encodedPassword = URLEncoder.encode(password, "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Can't happen since encoding is statically specified
        }

        // Only add the credentials to the playback URL if a
        // username and password are set in the current connection
        baseUrl = "http://";
        if (!encodedUsername.isEmpty()) {
            baseUrl += encodedUsername;
            if (!encodedPassword.isEmpty()) {
                baseUrl += ":" + encodedPassword + "@";
            }
        }
        baseUrl += address + ":" + streamingPort;

        switch (action) {
        case ACTION_PLAY:
            // Check if the recording exists in the download folder, if not
            // stream it from the server
            if (rec != null && app.isUnlocked()) {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, rec.getTitle() + ".mkv");
                logger.log(TAG, "initAction: Downloaded recording can be played from '" + file.getAbsolutePath()  + "': " + file.exists());
                if (file.exists()) {
                    startPlayback(file.getAbsolutePath(), "video/x-matroska");
                    break;
                }
            }

            // No downloaded recording exists, so continue starting the service
            // to loadRecordingById the url that shall be played. This could either be a
            // channel or a recording.
            Intent intent = new Intent(PlayActivity.this, EpgSyncService.class);
            intent.setAction("getTicket");
            intent.putExtras(getIntent().getExtras());
            this.startService(intent);
            break;

        case ACTION_CAST:
            if (ch != null && ch.getChannelNumber() > 0) {
                logger.log(TAG, "initAction: Starting to cast channel '" + ch.getChannelName() + "'");
                startCasting();
            } else if (rec != null) {
                logger.log(TAG, "initAction: Starting to cast recording '" + rec.getTitle() + "'");
                startCasting();
            }
            break;
        }
        logger.log(TAG, "initAction() returned");
    }

    @Override
    protected void onResume() {
        super.onResume();

        initAction();
    }

    /**
     * When the first ticket from the HTSService has been received the URL is
     * created together with the mime and profile data. This is then passed to
     * the startPlayback method which invokes the external media player.
     *
     * @param path   The path to the recording or channel that shall be played
     * @param ticket The ticket id that was given by the server
     */
    private void initPlayback(String path, String ticket) {
        logger.log(TAG, "initPlayback() called with: path = [" + path + "], ticket = [" + ticket + "]");
/*
        ServerProfile playbackProfile = new ProfileDataRepository(this).getPlaybackServerProfile();
        // Set default values if no profile was specified
        if (playbackProfile == null) {
            logger.log(TAG, "initPlayback: no profile defined, creating default profile");
            playbackProfile = new OldProfile();
        }

        // Set the correct MIME type. For 'pass' we assume MPEG-TS
        String mime = "application/octet-stream";
        switch (playbackTranscodingProfile.getContainer()) {
            case "mpegps":
                mime = "video/mp2p";
                break;
            case "mpegts":
                mime = "video/mp4";
                break;
            case "matroska":
                mime = "video/x-matroska";
                break;
            case "pass":
                mime = "video/mp2t";
                break;
            case "webm":
                mime = "video/webm";
                break;
        }

        // Create the URL for the external media player that is required to loadRecordingById
        // the stream from the server
        String playUrl = baseUrl + path + "?ticket=" + ticket;

        // If a profile was given, use it instead of the old values
        if (playbackProfile != null
                && playbackProfile.isEnabled()
                && serverStatus.getHtspVersion() >= 16
                && app.isUnlocked()) {
            playUrl += "&profile=" + playbackProfile.getName();
        } else {
            playUrl += "&mux=" + playbackProfile.container;
            if (playbackProfile.transcode) {
                playUrl += "&transcode=1";
                playUrl += "&resolution=" + playbackProfile.resolution;
                playUrl += "&acodec=" + playbackProfile.audio_codec;
                playUrl += "&vcodec=" + playbackProfile.video_codec;
                playUrl += "&scodec=" + playbackProfile.subtitle_codec;
            }
        }
        startPlayback(playUrl, mime);
        logger.log(TAG, "initPlayback() returned");

        */
    }

    /**
     * Starts the external media player with the given url and mime information.
     *
     * @param url  The url that shall be played. Can either be a http url or a local file path
     * @param mime The mime type that shall be used
     */
    private void startPlayback(String url, String mime) {

        // In case a local file will be played log the given url. In case a recording will be
        // streamed, create a special string for the logging without the http credentials
        String logUrl = url;
        if (url.indexOf('@') != -1) {
            logUrl = "http://<user>:<pass>" + url.substring(url.indexOf('@'));
        }

        logger.log(TAG, "startPlayback() called with: url = [" + logUrl + "], mime = [" + mime + "]");

        final Intent playbackIntent = new Intent(Intent.ACTION_VIEW);
        playbackIntent.setDataAndType(Uri.parse(url), mime);

        // Pass on the name of the channel or the recording title to the external 
        // video players. VLC uses itemTitle and MX Player the title string.
        playbackIntent.putExtra("itemTitle", title);
        playbackIntent.putExtra("title", title);

        // Start playing the video now in the UI thread
        this.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    logger.log(TAG, "startPlayback: Starting external player");
                    startActivity(playbackIntent);
                    finish();
                } catch (Throwable t) {
                    logger.log(TAG, "startPlayback: Can't execute external media player");

                    // Show a confirmation dialog before deleting the recording
                    new MaterialDialog.Builder(PlayActivity.this)
                        .title(R.string.no_media_player)
                        .content(R.string.show_play_store)
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                try {
                                    logger.log(TAG, "startPlayback: Starting play store to download external players");
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                    startActivity(installIntent);
                                } catch (Throwable t2) {
                                    logger.log(TAG, "startPlayback: Could not start google play store");
                                } finally {
                                    finish();
                                }
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                finish();
                            }
                        })
                        .show();
                }
            }
        });
        logger.log(TAG, "startPlayback() returned");
    }

    /**
     * Creates the url and other required media information for casting. This
     * information is then passed to the cast controller activity.
     */
    private void startCasting() {
        logger.log(TAG, "startCasting() called");

        String iconUrl = baseUrl + serverStatus.getWebroot();
        String castUrl = baseUrl + serverStatus.getWebroot();
        String subtitle = "";
        long duration = 0;
        int streamType = MediaInfo.STREAM_TYPE_NONE;

        if (ch != null) {
            castUrl += "/stream/channelnumber/" + ch.getChannelNumber();
            iconUrl += "/" + ch.getChannelIcon();
            streamType = MediaInfo.STREAM_TYPE_LIVE;
        } else if (rec != null) {
            castUrl += "/dvrfile/" + rec.getId();
            Channel channel = repository.getChannelByIdSync(rec.getChannelId());
            iconUrl += "/" + (channel != null ? channel.getChannelIcon() : "");
            streamType = MediaInfo.STREAM_TYPE_BUFFERED;

            if (rec.getSubtitle() != null) {
                subtitle = (rec.getSubtitle().length() > 0 ? rec.getSubtitle() : rec.getSummary());
            }
            duration = rec.getStop() - rec.getStart();
        }

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        // Check if the correct profile was set, if not use the default
        ConfigRepository configRepository = new ConfigRepository(this);
        ServerStatus serverStatus = configRepository.getServerStatus();
        ServerProfile castingProfile = configRepository.getCastingServerProfileById(serverStatus.getCastingServerProfileId());
        if (castingProfile == null) {
            castUrl += "?profile=" + Constants.CAST_PROFILE_DEFAULT;
        } else {
            castUrl += "?profile=" + castingProfile.getName();
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(castUrl)
            .setStreamType(streamType)
            .setContentType("video/webm")
            .setMetadata(movieMetadata)
            .setStreamDuration(duration)
            .build();

        logger.log(TAG, "startCasting: Casting the following program: title [" + title + "], url [" + castUrl + "]");

        RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        remoteMediaClient.load(mediaInfo, true, 0);

        logger.log(TAG, "startCasting() returned");
        finish();
    }

    /**
     * Called when an activity was closed and this one is active again
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_START_PLAYER) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_TICKET_ADD)) {
            HttpTicket t = (HttpTicket) obj;
            initPlayback(t.path, t.ticket);
        }
    }
}
