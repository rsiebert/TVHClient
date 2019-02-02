package org.tvheadend.tvhclient.features.dvr;

import android.content.Context;

import org.tvheadend.tvhclient.R;

public class RecordingUtils {

    public static String getPriorityName(Context context,  int priority) {
        String[] priorityNames = context.getResources().getStringArray(R.array.dvr_priority_names);
        if (priority >= 0 && priority <= 4) {
            return priorityNames[priority];
        } else if (priority == 6) {
            return priorityNames[5];
        } else {
            return "";
        }
    }

}
