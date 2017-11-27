package org.tvheadend.tvhclient.intent;

import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.activities.PlayActivity;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Model;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

public class PlayIntent extends Intent {

    public PlayIntent(Context ctx, Model m) {
        if (m instanceof Program) {
            new PlayIntent(ctx, (Program) m);
        } else if (m instanceof Recording) {
            new PlayIntent(ctx, (Recording) m);
        }
    }

    public PlayIntent(Context ctx, Program p) {
        super(ctx, PlayActivity.class);
        if (p != null && p.channel != null) {
            putExtra(Constants.BUNDLE_CHANNEL_ID, p.channel.id);
        }
    }

    public PlayIntent(Context ctx, Recording rec) {
        super(ctx, PlayActivity.class);
        if (rec != null) {
            putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
        }
    }

    public PlayIntent(Context ctx, Channel ch) {
        super(ctx, PlayActivity.class);
        if (ch != null) {
            putExtra(Constants.BUNDLE_CHANNEL_ID, ch.id);
        }
    }
}
