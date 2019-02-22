package org.tvheadend.tvhclient.ui.features.dialogs;

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;

public class GenreColorDialog {

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    public static boolean showDialog(Context context) {
        // Fill the list for the adapter
        GenreColorListAdapter adapter = new GenreColorListAdapter(context.getResources().getStringArray(R.array.pr_content_type0));
        new MaterialDialog.Builder(context)
                .title(R.string.genre_color_list)
                .adapter(adapter, null)
                .show();
        return true;
    }
}
