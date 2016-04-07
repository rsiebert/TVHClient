package org.tvheadend.tvhclient.intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.ExternalActionActivity;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Model;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.content.Context;
import android.content.Intent;

public class PlayIntent extends Intent {

    public PlayIntent(Context ctx, Model m) {
        super(ctx, ExternalActionActivity.class);
        if (m instanceof Program) {
            Program p = (Program) m;
            if (p != null && p.channel != null) {
                putExtra(Constants.BUNDLE_CHANNEL_ID, p.channel.id);
            }
        } else if (m instanceof Recording) {
            Recording rec = (Recording)m;
            if (rec != null) {
                putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
            }
        }
    }

    public PlayIntent(Context ctx, Program p) {
        super(ctx, ExternalActionActivity.class);
        if (p != null && p.channel != null) {
            putExtra(Constants.BUNDLE_CHANNEL_ID, p.channel.id);
        }
    }

    public PlayIntent(Context ctx, Recording rec) {
        super(ctx, ExternalActionActivity.class);
        if (rec != null) {
            putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
        }
    }

    public PlayIntent(Context ctx, Channel ch) {
        super(ctx, ExternalActionActivity.class);
        if (ch != null) {
            putExtra(Constants.BUNDLE_CHANNEL_ID, ch.id);
        }
    }
}
