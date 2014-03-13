/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
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
                setData(Uri.parse("http://akas.imdb.org/find?s=tt&q=" + url));
            }
        } catch (UnsupportedEncodingException e) {
            Log.i("SearchIMDbIntent", e.getLocalizedMessage());
        }
    }
}
