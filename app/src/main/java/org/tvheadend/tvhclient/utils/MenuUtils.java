package org.tvheadend.tvhclient.utils;


import android.app.Activity;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.adapter.GenreColorDialogAdapter;
import org.tvheadend.tvhclient.model.GenreColorDialogItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MenuUtils {

    private WeakReference<Activity> mActivity;

    public MenuUtils(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    public void handleMenuGenreColorSelection() {
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        final String[] s = activity.getResources().getStringArray(R.array.pr_content_type0);

        // Fill the list for the adapter
        final List<GenreColorDialogItem> items = new ArrayList<>();
        for (int i = 0; i < s.length; ++i) {
            GenreColorDialogItem genreColor = new GenreColorDialogItem();
            genreColor.color = MiscUtils.getGenreColor(activity, ((i + 1) * 16), 0);
            genreColor.genre = s[i];
            items.add(genreColor);
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.genre_color_list)
                .adapter(new GenreColorDialogAdapter(items), null)
                .show();
    }
}
