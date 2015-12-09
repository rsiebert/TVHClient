package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.HttpTicket;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

public class ExternalActionActivity extends Activity implements HTSListener {

    private final static String TAG = ExternalActionActivity.class.getSimpleName();

    private Context context;
    private TVHClientApplication app;
    private DatabaseHelper dbh;
    private DownloadManager dm;
    private int action;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;

        app = (TVHClientApplication) getApplication();
        dbh = DatabaseHelper.getInstance(this);
        action = getIntent().getIntExtra(Constants.BUNDLE_EXTERNAL_ACTION, Constants.EXTERNAL_ACTION_PLAY);

        // Check that a valid channel or recording was specified
        final Channel ch = app.getChannel(getIntent().getLongExtra(Constants.BUNDLE_CHANNEL_ID, 0));
        final Recording rec = app.getRecording(getIntent().getLongExtra(Constants.BUNDLE_RECORDING_ID, 0));
        if (ch == null && rec == null) {
            return;
        }

        // Start the service to get the url that shall be played
        Intent intent = new Intent(ExternalActionActivity.this, HTSService.class);
        intent.setAction(Constants.ACTION_GET_TICKET);
        intent.putExtras(getIntent().getExtras());
        this.startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    /**
     * When the first ticket from the HTSService has been received the URL is
     * created and then passed to the external media player.
     * 
     * @param path
     * @param ticket
     */
    private void startPlayback(String path, String ticket) {

        final Connection conn = dbh.getSelectedConnection();
        Profile profile = dbh.getProfile(conn.playback_profile_id);

        // Set default values if no profile was specified
        if (profile == null) {
            Log.d(TAG, "No profile was set for the selected connection.");
            profile = new Profile();
        }

        // Set the correct MIME type. For 'pass' we assume MPEG-TS
        String mime = "application/octet-stream";
        if (profile.container.equals("mpegps")) {
            mime = "video/mp2p";
        } else if (profile.container.equals("mpegts")) {
            mime = "video/mp4";
        } else if (profile.container.equals("matroska")) {
            mime = "video/x-matroska";
        } else if (profile.container.equals("pass")) {
            mime = "video/mp2t";
        } else if (profile.container.equals("webm")) {
            mime = "video/webm";
        }

        // Create the URL for the external media player that is required to get
        // the stream from the server
        String url = "http://" + conn.address + ":" + conn.streaming_port + path + "?ticket=" + ticket;

        // If a profile was given, use it instead of the old values
        if (profile.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            url += "&profile=" + profile.name;
        } else {
            url += "&mux=" + profile.container;
            if (profile.transcode) {
                url += "&transcode=1";
                url += "&resolution=" + profile.resolution;
                url += "&acodec=" + profile.audio_codec;
                url += "&vcodec=" + profile.video_codec;
                url += "&scodec=" + profile.subtitle_codec;
            }
        }

        switch (action) {
        case Constants.EXTERNAL_ACTION_PLAY:
            final Intent playbackIntent = new Intent(Intent.ACTION_VIEW);
            playbackIntent.setDataAndType(Uri.parse(url), mime);
            Log.d(TAG, "Playing url " + url);
    
            // Start playing the video now in the UI thread
            this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        Log.d(TAG, "Starting external player");
                        startActivity(playbackIntent);
                        finish();
                    } catch (Throwable t) {
                        Log.e(TAG, "Can't execute external media player", t);
    
                        // Show a confirmation dialog before deleting the recording
                        new MaterialDialog.Builder(context)
                            .title(R.string.no_media_player)
                            .content(R.string.show_play_store)
                            .positiveText(getString(android.R.string.yes))
                            .negativeText(getString(android.R.string.no))
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    try {
                                        Log.d(TAG, "Starting play store to download external players");
                                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                        installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                        startActivity(installIntent);
                                    } catch (Throwable t2) {
                                        Log.e(TAG, "Could not start google play store", t2);
                                    } finally {
                                        finish();
                                    }
                                }
                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    finish();
                                }
                            }).show();
                    }
                }
            });
            break;

        case Constants.EXTERNAL_ACTION_DOWNLOAD:
            dm = (DownloadManager) getSystemService(Service.DOWNLOAD_SERVICE);
            Request request = new Request(Uri.parse(url));
            //enqueue = dm.enqueue(request);
            break;
        }
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
            startPlayback(t.path, t.ticket);
        }
    }
}
