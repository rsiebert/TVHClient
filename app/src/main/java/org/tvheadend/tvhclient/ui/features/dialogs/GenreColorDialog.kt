package org.tvheadend.tvhclient.ui.features.dialogs

import android.content.Context

import com.afollestad.materialdialogs.MaterialDialog

import org.tvheadend.tvhclient.R

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    fun showGenreColorDialog(context: Context): Boolean {
        // Fill the list for the adapter
        val adapter = GenreColorListAdapter(context.resources.getStringArray(R.array.pr_content_type0))
        MaterialDialog.Builder(context)
                .title(R.string.genre_color_list)
                .adapter(adapter, null)
                .show()
        return true
    }
