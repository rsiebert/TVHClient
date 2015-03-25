package org.tvheadend.tvhclient.intent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class SearchIMDbIntent extends Intent {

    public SearchIMDbIntent(Context ctx, String query) {
        super(Intent.ACTION_VIEW);

        String url;
        try {
            // Try to encode the URL with the default character set.
            // Only continue if this was successful
            url = URLEncoder.encode(query, "utf-8");
            setData(Uri.parse("imdb:///find?s=tt&q=" + url));
            PackageManager packageManager = ctx.getPackageManager();
            if (packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                setData(Uri.parse("http://www.imdb.org/find?s=tt&q=" + url));
            }
        } catch (UnsupportedEncodingException e) {
            Log.i("SearchIMDbIntent", e.getLocalizedMessage());
        }
    }
}
