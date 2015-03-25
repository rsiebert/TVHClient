package org.tvheadend.tvhclient.intent;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.SearchResultActivity;
import org.tvheadend.tvhclient.model.Channel;

public class SearchEPGIntent extends Intent {

    public SearchEPGIntent(Context ctx, String query) {
        super(ctx, SearchResultActivity.class);
        setAction(Intent.ACTION_SEARCH);
        putExtra(SearchManager.QUERY, query);
    }

    public SearchEPGIntent(Context ctx, Channel ch, String query) {
        this(ctx, query);
        putExtra(Constants.BUNDLE_CHANNEL_ID, ch.id);
    }
}
