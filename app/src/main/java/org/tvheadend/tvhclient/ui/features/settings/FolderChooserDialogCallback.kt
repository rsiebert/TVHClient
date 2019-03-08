package org.tvheadend.tvhclient.ui.features.settings

import java.io.File

internal interface FolderChooserDialogCallback {
    fun onFolderSelected(folder: File)
}
