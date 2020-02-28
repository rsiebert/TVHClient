package org.tvheadend.tvhclient.ui.common.interfaces

import android.content.Intent
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.util.livedata.Event

interface SnackbarMessageInterface {

    fun setSnackbarMessage(intent: Intent)
    fun getSnackbarMessage(): LiveData<Event<Intent>>
}