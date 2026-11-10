package org.tvheadend.tvhclient.ui.features.navigation

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.channels.ChannelListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.CompletedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.FailedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RemovedRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.ScheduledRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingListFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingListFragment
import org.tvheadend.tvhclient.ui.features.epg.EpgFragment
import org.tvheadend.tvhclient.ui.features.information.HelpAndSupportFragment
import org.tvheadend.tvhclient.ui.features.information.StatusFragment
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment

/**
 * A single destination that can be selected from the navigation drawer.
 *
 * This is the single source of truth that used to be spread over four hand-maintained `when`
 * expressions in [NavigationDrawer]: the mapping from the drawer menu item to the destination, the
 * reverse mapping used to tick the selected item, and the fragment that a destination shows. Every
 * entry carries all of that in one place so the lists cannot drift apart.
 *
 * @property id Stable identifier that is persisted in the `start_screen` preference. It must not be
 *   renamed without a migration because it is written to disk (see `MigrateUtils`).
 * @property menuItemId The id of the entry in `res/menu/navigation_drawer.xml`.
 * @property titleRes The user visible name of the destination.
 * @property fragmentClass The fragment the destination shows, or `null` when the destination is not
 *   a fragment (currently only [SETTINGS], which launches an Activity).
 * @property fragmentFactory Creates a fresh instance of [fragmentClass], or `null` for the same
 *   reason.
 */
enum class NavDestination(
    val id: String,
    @IdRes val menuItemId: Int,
    @StringRes val titleRes: Int,
    val fragmentClass: Class<out Fragment>?,
    val fragmentFactory: (() -> Fragment)?,
) {
    CHANNELS("channels", R.id.nav_channels, R.string.channels, ChannelListFragment::class.java, ::ChannelListFragment),
    PROGRAM_GUIDE("program_guide", R.id.nav_program_guide, R.string.pref_program_guide, EpgFragment::class.java, ::EpgFragment),
    COMPLETED_RECORDINGS("completed_recordings", R.id.nav_completed_recordings, R.string.completed_recordings, CompletedRecordingListFragment::class.java, ::CompletedRecordingListFragment),
    SCHEDULED_RECORDINGS("scheduled_recordings", R.id.nav_scheduled_recordings, R.string.scheduled_recordings, ScheduledRecordingListFragment::class.java, ::ScheduledRecordingListFragment),
    SERIES_RECORDINGS("series_recordings", R.id.nav_series_recordings, R.string.series_recordings, SeriesRecordingListFragment::class.java, ::SeriesRecordingListFragment),
    TIMER_RECORDINGS("timer_recordings", R.id.nav_timer_recordings, R.string.timer_recordings, TimerRecordingListFragment::class.java, ::TimerRecordingListFragment),
    FAILED_RECORDINGS("failed_recordings", R.id.nav_failed_recordings, R.string.failed_recordings, FailedRecordingListFragment::class.java, ::FailedRecordingListFragment),
    REMOVED_RECORDINGS("removed_recordings", R.id.nav_removed_recordings, R.string.removed_recordings, RemovedRecordingListFragment::class.java, ::RemovedRecordingListFragment),
    STATUS("status", R.id.nav_status, R.string.status, StatusFragment::class.java, ::StatusFragment),
    UNLOCKER("additional_features", R.id.nav_extras, R.string.pref_unlocker, UnlockerFragment::class.java, ::UnlockerFragment),
    HELP("help", R.id.nav_help, R.string.help_and_support, HelpAndSupportFragment::class.java, ::HelpAndSupportFragment),

    /** Opens a separate Activity instead of showing a fragment, hence the `null` fragment fields. */
    SETTINGS("settings", R.id.nav_settings, R.string.settings, null, null);

    /**
     * Creates a new instance of the fragment this destination shows, or `null` when it does not show
     * one (see [SETTINGS]).
     */
    fun createFragment(): Fragment? = fragmentFactory?.invoke()

    companion object {

        /**
         * Resolves the destination a drawer menu item belongs to, or `null` when the item is not a
         * known destination.
         */
        fun fromMenuItemId(menuItemId: Int): NavDestination? =
            entries.firstOrNull { it.menuItemId == menuItemId }

        /**
         * Resolves the destination for the fragment that is currently shown, or `null` when the
         * fragment does not belong to a drawer destination. Used to tick the matching drawer item.
         *
         * [WebViewFragment] is mapped to [HELP] because the help screen can be shown either through
         * [HelpAndSupportFragment] or through the generic web view.
         */
        fun fromFragment(fragment: Fragment?): NavDestination? {
            if (fragment == null) {
                return null
            }
            // Match the concrete fragment first: some destinations (e.g. UnlockerFragment) extend
            // WebViewFragment, so the generic web view fallback below must not run before them.
            entries.firstOrNull { it.fragmentClass != null && it.fragmentClass.isInstance(fragment) }
                    ?.let { return it }
            return if (fragment is WebViewFragment) HELP else null
        }

        /**
         * Resolves the destination for the persisted start screen id, falling back to [CHANNELS] for
         * an unknown or missing id so an invalid preference can never break startup.
         */
        fun fromId(id: String?): NavDestination =
            entries.firstOrNull { it.id == id } ?: CHANNELS
    }
}
