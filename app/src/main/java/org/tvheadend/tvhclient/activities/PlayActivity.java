package org.tvheadend.tvhclient.activities;

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
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PlayActivity extends Activity implements HTSListener, OnRequestPermissionsResultCallback {

    private final static String TAG = PlayActivity.class.getSimpleName();

    private TVHClientApplication app;
    private int action;

    private Channel ch;
    private Recording rec;
    private String baseUrl;
    private Profile playbackProfile;
    private Profile castingProfile;
    private String username;
    private String password;
    private String address;
    private int streamingPort;
    private String title = "";
    private Logger logger;
    private DataStorage dataStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);

        app = TVHClientApplication.getInstance();
        logger = Logger.getInstance();
        dataStorage = DataStorage.getInstance();
        
        // If a play intent was sent no action is given, so default to play
        action = getIntent().getIntExtra(Constants.BUNDLE_ACTION, Constants.ACTION_PLAY);

        // Check that a valid channel or recording was specified
        ch = dataStorage.getChannel(getIntent().getLongExtra(Constants.BUNDLE_CHANNEL_ID, 0));
        rec = dataStorage.getRecording(getIntent().getLongExtra(Constants.BUNDLE_RECORDING_ID, 0));

        // Get the title from either the channel or recording
        if (ch != null) {
            title = ch.name;
        } else if (rec != null) {
            title = rec.title;
        } else {
            logger.log(TAG, "onCreate: No channel or recording provided, exiting");
            return;
        }

        // If the cast menu button is connected then assume playing means casting
        if (VideoCastManager.getInstance().isConnected()) {
            action = Constants.ACTION_CAST;
        }
    }

    /**
     *
     */
    private void initAction() {
        logger.log(TAG, "initAction() called");

        // Create the url with the credentials and the host and  
        // port configuration. This one is fixed for all actions
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
        Connection conn = databaseHelper.getSelectedConnection();
        if (conn != null) {
            username = conn.username;
            password = conn.password;
            address = conn.address;
            streamingPort = conn.streaming_port;
            playbackProfile = databaseHelper.getProfile(conn.playback_profile_id);
            castingProfile = databaseHelper.getProfile(conn.cast_profile_id);
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
        case Constants.ACTION_PLAY:
            // Check if the recording exists in the download folder, if not
            // stream it from the server
            if (rec != null && app.isUnlocked()) {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, rec.title + ".mkv");
                logger.log(TAG, "initAction: Downloaded recording can be played from '" + file.getAbsolutePath()  + "': " + file.exists());
                if (file.exists()) {
                    startPlayback(file.getAbsolutePath(), "video/x-matroska");
                    break;
                }
            }

            // No downloaded recording exists, so continue starting the service
            // to get the url that shall be played. This could either be a
            // channel or a recording.
            Intent intent = new Intent(PlayActivity.this, HTSService.class);
            intent.setAction("getTicket");
            intent.putExtras(getIntent().getExtras());
            this.startService(intent);
            break;

        case Constants.ACTION_CAST:
            if (ch != null && ch.number > 0) {
                logger.log(TAG, "initAction: Starting to cast channel '" + ch.name + "'");
                startCasting();
            } else if (rec != null) {
                logger.log(TAG, "initAction: Starting to cast recording '" + rec.title + "'");
                startCasting();
            }
            break;
        }
        logger.log(TAG, "initAction() returned");
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoCastManager.getInstance().incrementUiCounter();
        app.addListener(this);

        initAction();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoCastManager.getInstance().decrementUiCounter();
        app.removeListener(this);
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

        // Set default values if no profile was specified
        if (playbackProfile == null) {
            logger.log(TAG, "initPlayback: no profile defined, creating default profile");
            playbackProfile = new Profile();
        }

        // Set the correct MIME type. For 'pass' we assume MPEG-TS
        String mime = "application/octet-stream";
        switch (playbackProfile.container) {
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

        // Create the URL for the external media player that is required to get
        // the stream from the server
        String playUrl = baseUrl + path + "?ticket=" + ticket;

        // If a profile was given, use it instead of the old values
        if (playbackProfile.enabled
                && dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            playUrl += "&profile=" + playbackProfile.name;
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

        String iconUrl = baseUrl + dataStorage.getWebRoot();
        String castUrl = baseUrl + dataStorage.getWebRoot();
        String subtitle = "";
        long duration = 0;
        int streamType = MediaInfo.STREAM_TYPE_NONE;

        if (ch != null) {
            castUrl += "/stream/channelnumber/" + ch.number;
            iconUrl += "/" + ch.icon;
            streamType = MediaInfo.STREAM_TYPE_LIVE;
        } else if (rec != null) {
            castUrl += "/dvrfile/" + rec.id;
            iconUrl += "/" + (rec.channel != null ? rec.channel.icon : "");
            streamType = MediaInfo.STREAM_TYPE_BUFFERED;

            if (rec.subtitle != null) {
                subtitle = (rec.subtitle.length() > 0 ? rec.subtitle : rec.summary);
            }
            if (rec.stop != null && rec.start != null) {
                duration = rec.stop.getTime() - rec.start.getTime();
            }
        }

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        // Check if the correct profile was set, if not use the default
        if (castingProfile == null) {
            castUrl += "?profile=" + Constants.CAST_PROFILE_DEFAULT;
        } else {
            castUrl += "?profile=" + castingProfile.name;
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(castUrl)
            .setStreamType(streamType)
            .setContentType("video/webm")
            .setMetadata(movieMetadata)
            .setStreamDuration(duration)
            .build();

        logger.log(TAG, "startCasting: Casting the following program: title [" + title + "], url [" + castUrl + "]");
        VideoCastManager.getInstance().startVideoCastControllerActivity(this, mediaInfo, 0, true);

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

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_TICKET_ADD)) {
            HttpTicket t = (HttpTicket) obj;
            initPlayback(t.path, t.ticket);
        }
    }
}
