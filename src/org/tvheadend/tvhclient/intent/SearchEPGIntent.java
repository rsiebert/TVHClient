/*
 *  Copyright (C) 2014 Robert Siebert
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
