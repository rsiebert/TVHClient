package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.Stream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class PlaybackSelectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the information if we shall play a program or recording
        final TVHClientApplication app = (TVHClientApplication) getApplication();
        final Channel ch = app.getChannel(getIntent().getLongExtra(Constants.BUNDLE_CHANNEL_ID, 0));
        final Recording rec = app.getRecording(getIntent().getLongExtra(Constants.BUNDLE_RECORDING_ID, 0));
        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();

        // Set the required values of either the channel or the recording. If
        // none of the two is existing then do nothing an exit
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent(this, ExternalPlaybackActivity.class);
        intent.putExtra("serverHostPref", conn.address);
        intent.putExtra("httpPortPref", conn.streaming_port);
        
        if (ch != null) {
            // Pass on the channel id and the other settings
            intent.putExtra("channelId", ch.id);

            // Pass on the profile name, if allowed and available
            Profile p = DatabaseHelper.getInstance().getProfile(conn.playback_profile_id);
            if (p != null && app.isUnlocked()) {
                intent.putExtra("profile", p.name);
            }
            intent.putExtra("resolutionPref", Integer.parseInt(prefs.getString("progResolutionPref", "288")));
            intent.putExtra("transcodePref", prefs.getBoolean("progTranscodePref", true));
            intent.putExtra("acodecPref", prefs.getString("progAcodecPref", Stream.STREAM_TYPE_AAC));
            intent.putExtra("vcodecPref", prefs.getString("progVcodecPref", Stream.STREAM_TYPE_H264));
            intent.putExtra("scodecPref", prefs.getString("progScodecPref", "NONE"));
            intent.putExtra("containerPref", prefs.getString("progContainerPref", "matroska"));

        } else if (rec != null) {
            // Pass on the recording id and the other settings
            intent.putExtra("dvrId", rec.id);

            // Pass on the profile name, if allowed and available
            Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
            if (p != null && app.isUnlocked()) {
                intent.putExtra("profile", p.name);
            }
            intent.putExtra("resolutionPref", Integer.parseInt(prefs.getString("recResolutionPref", "288")));
            intent.putExtra("transcodePref", prefs.getBoolean("recTranscodePref", false));
            intent.putExtra("acodecPref", prefs.getString("recAcodecPref", Stream.STREAM_TYPE_AAC));
            intent.putExtra("vcodecPref", prefs.getString("recVcodecPref", Stream.STREAM_TYPE_MPEG4VIDEO));
            intent.putExtra("scodecPref", prefs.getString("recScodecPref", "PASS"));
            intent.putExtra("containerPref", prefs.getString("recContainerPref", "matroska"));
        } else {
            // This call should never be made 
            return;
        }

        // Now start the activity
        startActivity(intent);
        finish();
        return;
    }
}
