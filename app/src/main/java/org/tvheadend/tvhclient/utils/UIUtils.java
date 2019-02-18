package org.tvheadend.tvhclient.utils;

import android.content.Context;

import javax.annotation.Nullable;

import static org.tvheadend.tvhclient.utils.MiscUtils.convertUrlToHashString;

public class UIUtils {

    public static String getIconUrl(Context context, @Nullable final String url) {
        if (url == null) {
            return null;
        }
        return "file://" + context.getCacheDir() + "/" + convertUrlToHashString(url) + ".png";
    }

}
