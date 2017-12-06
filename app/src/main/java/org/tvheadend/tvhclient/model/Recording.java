package org.tvheadend.tvhclient.model;

import android.text.TextUtils;

import java.util.List;

public class Recording {

    public int id;                      // u32   required   ID of dvrEntry.
    public int channel;                 // u32   optional   Channel of dvrEntry.
    public long start;                  // s64   required   Time of when this entry was scheduled to start recording.
    public long stop;                   // s64   required   Time of when this entry was scheduled to stop recording.
    public long startExtra;             // s64   required   Extra start time (pre-time) in minutes (Added in version 13).
    public long stopExtra;              // s64   required   Extra stop time (post-time) in minutes (Added in version 13).
    public long retention;              // s64   required   DVR Entry retention time in days (Added in version 13).
    public int priority;                // u32   required   Priority (0 = Important, 1 = High, 2 = Normal, 3 = Low, 4 = Unimportant, 5 = Not set) (Added in version 13).
    public int eventId;                 // u32   optional   Associated EPG Event ID (Added in version 13).
    public String autorecId;            // str   optional   Associated Autorec UUID (Added in version 13).
    public String timerecId;            // str   optional   Associated Timerec UUID (Added in version 18).
    public int contentType;             // u32   optional   Content Type (like in the DVB standard) (Added in version 13).
    public String title;                // str   optional   Title of recording
    public String subtitle;             // str   optional   Subtitle of recording (Added in version 20).
    public String summary;              // str   optional   Short description of the recording (Added in version 6).
    public String description;          // str   optional   Long description of the recording.
    public String state;                // str   required   Recording state
    public String error;                // str   optional   Plain english error description (e.g. "Aborted by user").
    public String owner;                // str   optional   Name of the entry owner (Added in version 18).
    public String creator;              // str   optional   Name of the entry creator (Added in version 18).
    public String subscriptionError;    // str   optional   Subscription error string (Added in version 20).
    public String streamErrors;         // str   optional   Number of recording errors (Added in version 20).
    public String dataErrors;           // str   optional   Number of stream data errors (Added in version 20).
    public String path;                 // str   optional   Recording path for playback.
    public List<String> files;          // msg   optional   All recorded files for playback (Added in version 21).
    public long dataSize;               // s64   optional   Actual file size of the last recordings (Added in version 21).
    public int enabled;                 // u32   optional   Enabled flag (Added in version 23).
    public String episode;              // str   optional   Episode (Added in version 18).
    public String comment;              // str   optional   Comment (Added in version 18).

    public boolean isCompleted() {
        return error == null && TextUtils.equals(state, "completed");
    }

    public boolean isRecording() {
        return error == null && (TextUtils.equals(state, "recording") && !TextUtils.equals(state, "scheduled"));
    }

    public boolean isScheduled() {
        return error == null && (TextUtils.equals(state, "recording") || TextUtils.equals(state, "scheduled"));
    }

    public boolean isFailed() {
        return TextUtils.equals(state, "invalid");
    }

    public boolean isMissed() {
        return TextUtils.equals(state, "missed");
    }

    public boolean isAborted() {
        return TextUtils.equals(error, "Aborted by user") && TextUtils.equals(state, "completed");
    }

    public boolean isRemoved() {
        return TextUtils.equals(error, "File missing") && TextUtils.equals(state, "completed");
    }
}
