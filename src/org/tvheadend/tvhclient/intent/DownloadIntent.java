package org.tvheadend.tvhclient.intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.ExternalActionActivity;
import org.tvheadend.tvhclient.model.Recording;

import android.content.Context;
import android.content.Intent;

public class DownloadIntent extends Intent {

    public DownloadIntent(Context ctx, Recording rec) {
        super(ctx, ExternalActionActivity.class);
        if (rec != null) {
            putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            putExtra(Constants.BUNDLE_EXTERNAL_ACTION, Constants.EXTERNAL_ACTION_DOWNLOAD);
        }
    }
}
