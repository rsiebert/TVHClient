package org.tvheadend.tvhclient.intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.PlayActivity;
import org.tvheadend.tvhclient.model.Recording;

import android.content.Context;
import android.content.Intent;

public class DownloadIntent extends Intent {

    public DownloadIntent(Context ctx, Recording rec) {
        super(ctx, PlayActivity.class);
        if (rec != null) {
            putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            putExtra(Constants.BUNDLE_ACTION, Constants.ACTION_DOWNLOAD);
        }
    }
}
