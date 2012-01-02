/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.tvhguide.intent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import java.net.URLEncoder;

/**
 *
 * @author john-tornblom
 */
public class SearchIMDbIntent extends Intent {

    public SearchIMDbIntent(Context ctx, String query) {
        super(Intent.ACTION_VIEW);

        setData(Uri.parse("imdb:///find?q=" + URLEncoder.encode(query)));

        PackageManager packageManager = ctx.getPackageManager();
        if (packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            setData(Uri.parse("http://akas.imdb.org/find?q=" + URLEncoder.encode(query)));
        }
    }
}
