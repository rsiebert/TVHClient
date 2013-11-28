package org.tvheadend.tvhguide;

import org.tvheadend.tvhguide.model.Channel;
import org.tvheadend.tvhguide.model.Recording;
import org.tvheadend.tvhguide.model.Stream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class PlaybackSelectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the information if we shall play a program or recording
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        final Channel ch = app.getChannel(getIntent().getLongExtra("channelId", 0));
        final Recording rec = app.getRecording(getIntent().getLongExtra("dvrId", 0));

        Intent intent = null;

        // Set the required values of either the 
        // channel or the recording. If none of the 
        // two is existing then do nothing an exit
        if (ch != null) {
            Log.i("PlaybackSelectionActivity", "Playing program");
            
            // Check if an external player shall be used
            if (prefs.getBoolean("progExternalPref", false)) {
                intent = new Intent(this, ExternalPlaybackActivity.class);
            } else {
                intent = new Intent(this, PlaybackActivity.class);
                intent.putExtra("title", ch.name);
            }

            // Pass on the channel id and the other settings
            intent.putExtra("channelId", ch.id);
            intent.putExtra("serverHostPref", prefs.getString("progServerHostPref", "localhost"));
            intent.putExtra("httpPortPref", Integer.parseInt(prefs.getString("progHttpPortPref", "9981")));
            intent.putExtra("resolutionPref", Integer.parseInt(prefs.getString("progResolutionPref", "288")));
            intent.putExtra("transcodePref", prefs.getBoolean("progTranscodePref", true));
            intent.putExtra("acodecPref", prefs.getString("progAcodecPref", Stream.STREAM_TYPE_AAC));
            intent.putExtra("vcodecPref", prefs.getString("progVcodecPref", Stream.STREAM_TYPE_H264));
            intent.putExtra("scodecPref", prefs.getString("progScodecPref", "NONE"));
            intent.putExtra("containerPref", prefs.getString("progContainerPref", "matroska"));
        }
        else if (rec != null) {
            Log.i("PlaybackSelectionActivity", "Playing recording");
            
            // Check if an external player shall be used
            if (prefs.getBoolean("recExternalPref", false)) {
                intent = new Intent(this, ExternalPlaybackActivity.class);
            } else {
                intent = new Intent(this, PlaybackActivity.class);
                intent.putExtra("title", rec.title);
            }

            // Pass on the recording id and the other settings
            intent.putExtra("dvrId", rec.id);
            intent.putExtra("serverHostPref", prefs.getString("recServerHostPref", "localhost"));
            intent.putExtra("httpPortPref", Integer.parseInt(prefs.getString("recHttpPortPref", "9981")));
            intent.putExtra("resolutionPref", Integer.parseInt(prefs.getString("recResolutionPref", "288")));
            intent.putExtra("transcodePref", prefs.getBoolean("recTranscodePref", false));
            intent.putExtra("acodecPref", prefs.getString("recAcodecPref", Stream.STREAM_TYPE_AAC));
            intent.putExtra("vcodecPref", prefs.getString("recVcodecPref", Stream.STREAM_TYPE_MPEG4VIDEO));
            intent.putExtra("scodecPref", prefs.getString("recScodecPref", "PASS"));
            intent.putExtra("containerPref", prefs.getString("recContainerPref", "matroska"));
        }

        // Now start the activity
        if (intent != null) {
            startActivity(intent);
            finish();
        }
        return;
    }
}
