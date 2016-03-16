package org.tvheadend.tvhclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

public class ExternalActionActivity extends Activity implements HTSListener {

    private final static String TAG = ExternalActionActivity.class.getSimpleName();

    private TVHClientApplication app;
    private DatabaseHelper dbh;
    private Connection conn;
    private DownloadManager dm;
    private int action;

    private Channel ch;
    private Recording rec;
    private String baseUrl;

    private String title = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (TVHClientApplication) getApplication();
        dbh = DatabaseHelper.getInstance(this);
        conn = dbh.getSelectedConnection();
        action = getIntent().getIntExtra(Constants.BUNDLE_EXTERNAL_ACTION, Constants.EXTERNAL_ACTION_PLAY);

        // Check that a valid channel or recording was specified
        ch = app.getChannel(getIntent().getLongExtra(Constants.BUNDLE_CHANNEL_ID, 0));
        rec = app.getRecording(getIntent().getLongExtra(Constants.BUNDLE_RECORDING_ID, 0));

        // Get the title from either the channel or recording
        if (ch != null) {
            title = ch.name;
        } else if (rec != null) {
            title = rec.title;
        }

        // If the cast menu button is connected then assume playing means casting
        if (VideoCastManager.getInstance().isConnected()) {
            action = Constants.EXTERNAL_ACTION_CAST;
        }

        // Create the url with the credentials and the host and  
        // port configuration. This one is fixed for all actions
        baseUrl = "http://" + conn.username + ":" + conn.password + "@" + conn.address + ":" + conn.streaming_port;

        switch (action) {
        case Constants.EXTERNAL_ACTION_PLAY:
            // Start the service to get the url that shall be played
            Intent intent = new Intent(ExternalActionActivity.this, HTSService.class);
            intent.setAction(Constants.ACTION_GET_TICKET);
            intent.putExtras(getIntent().getExtras());
            this.startService(intent);
            break;

        case Constants.EXTERNAL_ACTION_DOWNLOAD:
            if (rec != null) {
                startDownload();
            }
            break;

        case Constants.EXTERNAL_ACTION_CAST:
            if (ch != null) {
                // In case a channel shall be played the channel uuid is required. 
                // It is not provided via the HTSP API, only via the webinterface 
                // API. Load the channel UUIDs via server:host/api/epg/events/grid.
                if (ch.uuid != null && ch.uuid.length() > 0) {
                    startCasting();
                } else {
                    new ChannelUuidLoaderTask().execute(conn);
                }
            } else if (rec != null) {
                startCasting();
            }
            break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.addListener(this);
        VideoCastManager.getInstance().incrementUiCounter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.removeListener(this);
        VideoCastManager.getInstance().decrementUiCounter();
    }

    /**
     * Downloads the defined recording via the internal download manager. 
     * The user will be notified by the system that a download is progressing.
     */
    private void startDownload() {

        String downloadUrl = "http://" + conn.address + ":" + conn.streaming_port + "/dvrfile/" + rec.id;
        String auth = "Basic " + Base64.encodeToString((conn.username + ":" + conn.password).getBytes(), Base64.NO_WRAP);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.addRequestHeader("Authorization", auth);
        request.setTitle(getString(R.string.download));
        request.setDescription(rec.title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Save the downloaded file in the external download storage
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_download_to_external_storage", false)) {
            app.log(TAG, "Saving the download to the external storage");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, rec.title.replace(' ', '_'));
        }

        dm = (DownloadManager) getSystemService(Service.DOWNLOAD_SERVICE);
        final long id = dm.enqueue(request);

        app.log(TAG, "Started download with id " + id + " from url " + downloadUrl);

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage
        // space. The download manager does not sent a broadcast if this error
        // occurs.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor c = dm.query(query);
                while (c.moveToNext()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    app.log(TAG, "Download " + id + " status is " + status + ", reason " + reason);

                    switch (status) {
                    case DownloadManager.STATUS_FAILED:
                        // Check the reason value if it is insufficient storage space
                        if (reason == 1006) {
                            app.log(TAG, "Download " + id + " failed due to insufficient storage space");
                            showErrorDialog(getString(R.string.download_error_insufficient_space, rec.title));
                        } else if (reason == 407) {
                            app.log(TAG, "Download " + id + " failed due to missing / wrong authentication");
                            showErrorDialog(getString(R.string.download_error_authentication_required, rec.title));
                        } else {
                            finish();
                        }
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        app.log(TAG, "Download " + id + " paused!");
                        break;

                    case DownloadManager.STATUS_PENDING:
                        app.log(TAG, "Download " + id + " pending!");
                        break;

                    case DownloadManager.STATUS_RUNNING:
                        app.log(TAG, "Download " + id + " in progress!");
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        app.log(TAG, "Download " + id + " complete!");
                        break;

                    default:
                        finish();
                        break;
                    }
                }
            }
        }, 2500);
    }

    /**
     * When the first ticket from the HTSService has been received the URL is
     * created and then passed to the external media player.
     * 
     * @param path
     * @param ticket
     */
    private void startPlayback(String path, String ticket) {

        // Set default values if no profile was specified
        Profile profile = dbh.getProfile(conn.playback_profile_id);
        if (profile == null) {
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
        String playUrl = baseUrl + path + "?ticket=" + ticket;

        // If a profile was given, use it instead of the old values
        if (profile != null  
                && profile.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            playUrl += "&profile=" + profile.name;
        } else {
            playUrl += "&mux=" + profile.container;
            if (profile.transcode) {
                playUrl += "&transcode=1";
                playUrl += "&resolution=" + profile.resolution;
                playUrl += "&acodec=" + profile.audio_codec;
                playUrl += "&vcodec=" + profile.video_codec;
                playUrl += "&scodec=" + profile.subtitle_codec;
            }
        }

        final Intent playbackIntent = new Intent(Intent.ACTION_VIEW);
        playbackIntent.setDataAndType(Uri.parse(playUrl), mime);

        // Pass on the name of the channel or the recording title to the external 
        // video players. VLC uses itemTitle and MX Player the title string.
        playbackIntent.putExtra("itemTitle", title);
        playbackIntent.putExtra("title", title);

        app.log(TAG, "Starting to play from url " + playUrl);

        // Start playing the video now in the UI thread
        this.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    app.log(TAG, "Starting external player");
                    startActivity(playbackIntent);
                    finish();
                } catch (Throwable t) {
                    app.log(TAG, "Can't execute external media player");

                    // Show a confirmation dialog before deleting the recording
                    new MaterialDialog.Builder(ExternalActionActivity.this)
                        .title(R.string.no_media_player)
                        .content(R.string.show_play_store)
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                try {
                                    app.log(TAG, "Starting play store to download external players");
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                    startActivity(installIntent);
                                } catch (Throwable t2) {
                                    app.log(TAG, "Could not start google play store");
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
    }

    /**
     * 
     */
    private void startCasting() {

        String iconUrl = "";
        String castUrl = "";
        String subtitle = "";
        long duration = 0;

        if (ch != null) {
            castUrl = baseUrl + "/stream/channel/" + ch.uuid;
            iconUrl = baseUrl + "/" + ch.icon;
        } else if (rec != null) {
            castUrl = baseUrl + "/dvrfile/" + rec.id;
            subtitle = (rec.subtitle.length() > 0 ? rec.subtitle : rec.summary);
            duration = rec.stop.getTime() - rec.start.getTime();
            iconUrl = baseUrl + "/" + (rec.channel != null ? rec.channel.icon : "");
        }

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);        
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // small cast icon
        movieMetadata.addImage(new WebImage(Uri.parse(iconUrl)));   // large background icon

        // Check if the correct profile was set, if not try to do this
        castUrl += "?profile=" + dbh.getProfile(conn.cast_profile_id).name;

        app.log(TAG, "Cast title is " + title);
        app.log(TAG, "Cast subtitle is " + subtitle);
        app.log(TAG, "Cast icon is " + iconUrl);
        app.log(TAG, "Cast url is " + castUrl);

        MediaInfo mediaInfo = new MediaInfo.Builder(castUrl.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/webm")
            .setMetadata(movieMetadata)
            .setStreamDuration(duration)
            .build();

        VideoCastManager.getInstance().startVideoCastControllerActivity(this, mediaInfo, 0, true);
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
            startPlayback(t.path, t.ticket);
        }
    }

    /**
     * Task that opens a connection to the defined URL, authenticates and
     * downloads the available EPG data. If successful the JSON formatted
     * data will be parsed for the available channel UUID.
     * 
     * @author rsiebert
     *
     */
    class ChannelUuidLoaderTask extends AsyncTask<Connection, Void, String> {

        protected void onPreExecute() {
            // TODO Show loading indicator
        }

        protected String doInBackground(Connection... conns) {
            InputStream is = null;
            String result = "";
            try {
                Connection c = conns[0];
                String url = "http://" + c.address + ":" + c.streaming_port + "/api/epg/events/grid";
                String auth = "Basic " + Base64.encodeToString((c.username + ":" + c.password).getBytes(), Base64.NO_WRAP);

                app.log(TAG, "Connecting to " + url);

                HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("Authorization", auth);
                conn.connect();

                is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                result = sb.toString();

            } catch (Throwable tr) {
                app.log(TAG, tr.getLocalizedMessage());
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // NOP
                }
            }
            return result;
        }

        protected void onPostExecute(String result) {

            if (result.length() == 0) {
                app.log(TAG, "Error loading JSON data");
                showErrorDialog(getString(R.string.error_loading_json_data));
                return;
            }

            try {
                app.log(TAG, "Parsing JSON data");
                JSONObject jsonObj = new JSONObject(result);

                // Get the JSON array node and loop through all 
                // entries to save the UUIDs for all channels.
                JSONArray epgData = jsonObj.getJSONArray("entries");
                for (int i = 0; i < epgData.length(); i++) {
                    JSONObject epgItem = epgData.getJSONObject(i);
                    String uuid = epgItem.getString("channelUuid");
                    String name = epgItem.getString("channelName");
                    for (Channel ch : app.getChannels()) {
                        if (ch.name.equals(name)) {
                            ch.uuid = uuid;
                            break;
                        }
                    }
                }

            } catch (JSONException e) {
                app.log(TAG, e.getLocalizedMessage());
            } finally {
                // Either start casting with the found UUID or 
                // show a message that the UUID could not be found
                if (ch.uuid != null && ch.uuid.length() > 0) {
                    startCasting();
                } else {
                    app.log(TAG, "Error parsing JSON data");
                    showErrorDialog(getString(R.string.error_parsing_json_data));
                    return;
                }
            }
        }
    }

    private void showErrorDialog(String msg) {
        new MaterialDialog.Builder(this)
                .content(msg)
                .positiveText("Close")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        finish();
                    }
                }).show();
    }
}
