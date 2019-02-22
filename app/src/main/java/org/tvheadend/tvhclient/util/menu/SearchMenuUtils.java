package org.tvheadend.tvhclient.util.menu;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.features.search.SearchActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchMenuUtils {

    public static boolean onMenuSelected(@NonNull Context context, int menuItemId, @Nullable String title) {
        return onMenuSelected(context, menuItemId, title, 0);
    }

    public static boolean onMenuSelected(@NonNull Context context, int menuItemId, @Nullable String title, int channelId) {
        if (title == null || title.length() == 0) {
            return false;
        }
        switch (menuItemId) {
            case R.id.menu_search_imdb:
                return searchImdbWebsite(context, title);

            case R.id.menu_search_fileaffinity:
                return searchFileAffinityWebsite(context, title);

            case R.id.menu_search_youtube:
                return searchYoutube(context, title);

            case R.id.menu_search_google:
                return searchGoogle(context, title);

            case R.id.menu_search_epg:
                return searchEpgSelection(context, title, channelId);
        }
        return false;
    }

    private static boolean searchYoutube(@NonNull Context context, @NonNull String title) {
        try {
            String url = URLEncoder.encode(title, "utf-8");
            // Search for the given title using the installed youtube application
            Intent intent = new Intent(Intent.ACTION_SEARCH, Uri.parse("vnd.youtube:"));
            intent.putExtra("query", url);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager packageManager = context.getPackageManager();
            if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                // No app is installed, fall back to the website version
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.youtube.com/results?search_query=" + url));
            }
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    private static boolean searchGoogle(@NonNull Context context, @NonNull String title) {
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.google.com/search?q=" + url));
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    private static boolean searchImdbWebsite(@NonNull Context context, @NonNull String title) {
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///find?s=tt&q=" + url));
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                intent.setData(Uri.parse("http://www.imdb.com/find?s=tt&q=" + url));
                context.startActivity(intent);
            }
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    private static boolean searchFileAffinityWebsite(@NonNull Context context, @NonNull String title) {
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.filmaffinity.com/es/search.php?stext=" + url));
            context.startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    private static boolean searchEpgSelection(@NonNull Context context, @NonNull String title, int channelId) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, title);
        intent.putExtra("type", "program_guide");
        if (channelId > 0) {
            intent.putExtra("channelId", channelId);
        }
        context.startActivity(intent);
        return true;
    }
}
