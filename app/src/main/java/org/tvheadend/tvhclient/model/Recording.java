package org.tvheadend.tvhclient.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Recording extends Model implements Comparable<Recording> {

    public Date start;
    public Date stop;
    public String title;
    public String subtitle;
    public String summary;
    public String description;
    public Channel channel;
    public String state;
    public String error;
    public long eventId;
    public String autorecId = null;
    public String timerecId = null;
    public long startExtra;
    public long stopExtra;
    public long retention;
    public long priority;
    public long contentType;
    public String comment;
    public String episode;
    public final List<DvrCutpoint> dvrCutPoints = new ArrayList<>();
    public String subscriptionError;
    public long streamErrors;
    public long dataErrors;
    public long dataSize;
    public boolean enabled;

    private final int COMPLETED = 1;
    private final int RECORDING = 2;
    private final int SCHEDULED = 3;
    private final int MISSED = 4;
    private final int REMOVED = 5;
    private final int FAILED = 6;
    private final int ABORTED = 7;
    public String owner;
    public String creator;
    public String path;
    public String files;

    @Override
    public int compareTo(@NonNull Recording that) {
        if (this.getState() == 1 && that.getState() == 1) {
            return this.start.compareTo(that.start);
        } else {
            return that.start.compareTo(this.start);
        }
    }

    public boolean isCompleted() {
        return getState() == COMPLETED;
    }

    public boolean isRecording() {
        return getState() == RECORDING;
    }

    public boolean isScheduled() {
        return getState() == SCHEDULED;
    }

    public boolean isMissed() {
        return getState() == MISSED;
    }

    public boolean isRemoved() {
        return getState() == REMOVED;
    }

    public boolean isFailed() {
        return getState() == FAILED;
    }

    public boolean isAborted() {
        return getState() == ABORTED;
    }

    private int getState() {
        // The server should always provide a state, everything
        // else is considered as unknown and should not happen.
        if (state != null) {
            // Handle the positive states first
            if (error == null) {
                // Recordings without a missing file will be considered as completed.
                // If the file is missing they are treated as removed recordings. If completed
                // but another error is set, they are shown in the failed recordings
                switch (state) {
                    case "completed":
                        return COMPLETED;
                    case "missed":
                        return MISSED;
                    case "recording":
                        return RECORDING;
                    case "scheduled":
                        return SCHEDULED;
                }
            } else {
                // Consider the recording as deleted / removed if it was completed and the file is
                // missing. Failed recordings are all recordings that are either missing or invalid
                if (error.equals("File missing") && state.equals("completed")) {
                    return REMOVED;
                } else if (error.equals("Aborted by user") && state.equals("completed")) {
                    return ABORTED;
                } else if (state.equals("missed") || state.equals("invalid")) {
                    return FAILED;
                }
            }
        }
        // All other cases are unknown;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Recording && ((Recording) o).id == id;
    }
}
