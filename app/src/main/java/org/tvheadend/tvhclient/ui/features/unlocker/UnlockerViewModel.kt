package org.tvheadend.tvhclient.ui.features.unlocker

import android.app.Application
import org.tvheadend.tvhclient.ui.base.BaseViewModel

/**
 * Owns the state of the unlocker screen.
 *
 * The unlock state is taken from the billing data source instead of a fragment argument, so the
 * screen is correct no matter how it was opened and it updates as soon as the purchase is
 * acknowledged by Google Play.
 */
class UnlockerViewModel(application: Application) : BaseViewModel(application)
