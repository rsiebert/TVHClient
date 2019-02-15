package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;

import javax.annotation.Nullable;

import static org.tvheadend.tvhclient.utils.MiscUtils.convertUrlToHashString;

public class UIUtils {

    // Constants required for the date calculation
    private static final int TWO_DAYS = 1000 * 3600 * 24 * 2;
    private static final int SIX_DAYS = 1000 * 3600 * 24 * 6;

    private UIUtils() {
        throw new IllegalAccessError("Utility class");
    }

    static int getGenreColor(final Context context, final int contentType, final int offset) {
        if (contentType < 0) {
            // Return a fully transparent color in case no genre is available
            return context.getResources().getColor(android.R.color.transparent);
        } else {
            // Get the genre color from the content type
            int color = R.color.EPG_OTHER;
            int type = (contentType / 16);
            switch (type) {
                case 0:
                    color = R.color.EPG_MOVIES;
                    break;
                case 1:
                    color = R.color.EPG_NEWS;
                    break;
                case 2:
                    color = R.color.EPG_SHOWS;
                    break;
                case 3:
                    color = R.color.EPG_SPORTS;
                    break;
                case 4:
                    color = R.color.EPG_CHILD;
                    break;
                case 5:
                    color = R.color.EPG_MUSIC;
                    break;
                case 6:
                    color = R.color.EPG_ARTS;
                    break;
                case 7:
                    color = R.color.EPG_SOCIAL;
                    break;
                case 8:
                    color = R.color.EPG_SCIENCE;
                    break;
                case 9:
                    color = R.color.EPG_HOBBY;
                    break;
                case 10:
                    color = R.color.EPG_SPECIAL;
                    break;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // Get the color with the desired alpha value
            int c = context.getResources().getColor(color);
            int alpha = (int) (((float) prefs.getInt("genre_color_transparency", 70)) / 100.0f * 255.0f);
            if (alpha >= offset) {
                alpha -= offset;
            }
            return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
        }
    }

    public static String getIconUrl(Context context, @Nullable final String url) {
        if (url == null) {
            return null;
        }
        return "file://" + context.getCacheDir() + "/" + convertUrlToHashString(url) + ".png";
    }

}
