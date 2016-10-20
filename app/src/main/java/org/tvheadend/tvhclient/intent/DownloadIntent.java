package org.tvheadend.tvhclient.intent;

import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DownloadActivity;
import org.tvheadend.tvhclient.model.Recording;

public class DownloadIntent extends Intent {

    public DownloadIntent(Context ctx, Recording rec) {
        super(ctx, DownloadActivity.class);
        if (rec != null) {
            putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            putExtra(Constants.BUNDLE_ACTION, Constants.ACTION_DOWNLOAD);
        }
    }
}
