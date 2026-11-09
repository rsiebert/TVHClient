package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
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
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment
import org.tvheadend.tvhclient.util.getThemeId
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Wraps the [NavigationView] based navigation drawer. Replaces the former MaterialDrawer
 * implementation with the framework/Material Components drawer.
 */
class NavigationDrawer(private val activity: AppCompatActivity,
                       toolbar: Toolbar,
                       private val drawerLayout: DrawerLayout,
                       private val navigationView: NavigationView,
                       private val navigationViewModel: NavigationViewModel,
                       statusViewModel: StatusViewModel,
                       private val isDualPane: Boolean) {

    private val toggle: ActionBarDrawerToggle =
            ActionBarDrawerToggle(activity, drawerLayout, toolbar, R.string.app_name, R.string.app_name)

    private val connectionSpinner: Spinner =
            navigationView.getHeaderView(0).findViewById(R.id.navigation_header_connection_spinner)

    private var connectionIds: List<Int> = emptyList()

    private val countItemIds = intArrayOf(
            R.id.nav_channels,
            R.id.nav_series_recordings,
            R.id.nav_timer_recordings,
            R.id.nav_completed_recordings,
            R.id.nav_scheduled_recordings,
            R.id.nav_failed_recordings,
            R.id.nav_removed_recordings)

    private val baseTitles = mutableMapOf<Int, CharSequence>()

    init {
        toggle.syncState()
        toggle.setToolbarNavigationClickListener { activity.onBackPressedDispatcher.onBackPressed() }

        countItemIds.forEach { id ->
            navigationView.menu.findItem(id)?.title?.let { baseTitles[id] = it }
        }

        navigationView.getHeaderView(0).setBackgroundResource(
                if (getThemeId(activity) == R.style.CustomTheme_Light) R.drawable.header_light else R.drawable.header_dark)

        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(navigationView)
            navigationViewModel.setNavigationMenuId(menuIdForItem(item.itemId))
            true
        }

        navigationViewModel.isUnlockedLiveData.observe(activity) { unlocked ->
            navigationView.menu.findItem(R.id.nav_extras)?.isVisible = !unlocked
        }
        navigationViewModel.connectionLiveData.observe(activity) {
            updateConnectionSpinner()
        }
        navigationViewModel.connections.observe(activity) {
            updateConnectionSpinner()
        }

        connectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onConnectionSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        statusViewModel.channelCount.observe(activity) { setBadge(R.id.nav_channels, it) }
        statusViewModel.seriesRecordingCount.observe(activity) { setBadge(R.id.nav_series_recordings, it) }
        statusViewModel.timerRecordingCount.observe(activity) { setBadge(R.id.nav_timer_recordings, it) }
        statusViewModel.completedRecordingCount.observe(activity) { setBadge(R.id.nav_completed_recordings, it) }
        statusViewModel.scheduledRecordingCount.observe(activity) { setBadge(R.id.nav_scheduled_recordings, it) }
        statusViewModel.failedRecordingCount.observe(activity) { setBadge(R.id.nav_failed_recordings, it) }
        statusViewModel.removedRecordingCount.observe(activity) { setBadge(R.id.nav_removed_recordings, it) }

        updateConnectionSpinner()
    }

    private fun setBadge(itemId: Int, count: Int) {
        val item = navigationView.menu.findItem(itemId) ?: return
        val baseTitle = baseTitles[itemId] ?: item.title
        item.title = if (count > 0) "$baseTitle ($count)" else baseTitle
    }

    private fun updateConnectionSpinner() {
        val connections = navigationViewModel.connections.value ?: emptyList()
        connectionIds = connections.map { it.id }
        val names = if (connections.isEmpty()) {
            listOf(activity.getString(R.string.no_connection_available))
        } else {
            connections.map { it.name ?: "" }
        }
        val adapter = ArrayAdapter(activity, R.layout.navigation_header_connection_item, names)
        connectionSpinner.adapter = adapter

        val activeIndex = connections.indexOfFirst { it.id == activeConnectionId() }
        if (activeIndex >= 0) {
            connectionSpinner.setSelection(activeIndex, false)
        }
    }

    private fun activeConnectionId(): Int = navigationViewModel.connectionLiveData.value?.id ?: -1

    private fun onConnectionSelected(position: Int) {
        val selectedId = connectionIds.getOrNull(position) ?: return
        // Ignore the callback that fires when the spinner is programmatically set to the
        // currently active connection.
        if (selectedId == activeConnectionId()) {
            return
        }

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.connect_to_new_server)
                .setNegativeButton(R.string.cancel) { _, _ ->
                    val activeIndex = connectionIds.indexOf(activeConnectionId())
                    if (activeIndex >= 0) {
                        connectionSpinner.setSelection(activeIndex, false)
                    }
                }
                .setPositiveButton(R.string.connect) { _, _ ->
                    activity.lifecycleScope.launch {
                        if (navigationViewModel.setSelectedConnectionAsActive(selectedId)) {
                            navigationViewModel.updateConnectionAndRestartApplication(activity)
                        }
                    }
                }
                .setCancelable(false)
                .show()
    }

    fun getSelectedMenu(): Int =
            navigationView.checkedItem?.let { menuIdForItem(it.itemId) } ?: MENU_CHANNELS

    fun saveInstanceState(outState: Bundle): Bundle {
        // The DrawerLayout and NavigationView persist their own state through their view ids.
        return outState
    }

    fun enableDrawerIndicator(isEnabled: Boolean) {
        toggle.isDrawerIndicatorEnabled = isEnabled
    }

    fun setSelectedNavigationDrawerMenuFromFragmentType(fragment: Fragment?) {
        val menuId = when (fragment) {
            is ChannelListFragment -> MENU_CHANNELS
            is EpgFragment -> MENU_PROGRAM_GUIDE
            is CompletedRecordingListFragment -> MENU_COMPLETED_RECORDINGS
            is ScheduledRecordingListFragment -> MENU_SCHEDULED_RECORDINGS
            is SeriesRecordingListFragment -> MENU_SERIES_RECORDINGS
            is TimerRecordingListFragment -> MENU_TIMER_RECORDINGS
            is FailedRecordingListFragment -> MENU_FAILED_RECORDINGS
            is RemovedRecordingListFragment -> MENU_REMOVED_RECORDINGS
            is StatusFragment -> MENU_STATUS
            is UnlockerFragment -> MENU_UNLOCKER
            is WebViewFragment -> MENU_HELP
            else -> return
        }
        navigationView.setCheckedItem(itemIdForMenu(menuId))
    }

    /**
     * Creates and returns a new fragment that is associated with the given menu
     */
    private fun getFragmentFromSelectedNavigationDrawerMenu(position: Int): Fragment? {
        return when (position) {
            MENU_CHANNELS -> ChannelListFragment()
            MENU_PROGRAM_GUIDE -> EpgFragment()
            MENU_COMPLETED_RECORDINGS -> CompletedRecordingListFragment()
            MENU_SCHEDULED_RECORDINGS -> ScheduledRecordingListFragment()
            MENU_SERIES_RECORDINGS -> SeriesRecordingListFragment()
            MENU_TIMER_RECORDINGS -> TimerRecordingListFragment()
            MENU_FAILED_RECORDINGS -> FailedRecordingListFragment()
            MENU_REMOVED_RECORDINGS -> RemovedRecordingListFragment()
            MENU_UNLOCKER -> UnlockerFragment()
            MENU_HELP -> HelpAndSupportFragment()
            MENU_STATUS -> StatusFragment()
            else -> null
        }
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param id Selected position within the menu array
     */
    fun handleDrawerItemSelected(id: Int) {
        Timber.d("Handling new navigation menu id $id")

        if (id == MENU_SETTINGS) {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
            return
        }

        // A new or existing main fragment shall be shown. So save the menu position so we
        // know which one was selected. Additionally remove any old details fragment in case
        // dual pane mode is active to prevent showing wrong details data.
        // Finally show the new main fragment and add it to the back stack
        // only if it is a new fragment and not an existing one.
        val fragment = getFragmentFromSelectedNavigationDrawerMenu(id)
        if (fragment != null) {
            if (isDualPane) {
                val detailsFragment = activity.supportFragmentManager.findFragmentById(R.id.details)
                if (detailsFragment != null) {
                    activity.supportFragmentManager.beginTransaction().remove(detailsFragment).commit()
                }
            }
            activity.supportFragmentManager.beginTransaction().replace(R.id.main, fragment).let {
                val addFragmentToBackStack = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("navigation_history_enabled", activity.resources.getBoolean(R.bool.pref_default_navigation_history_enabled))
                if (addFragmentToBackStack) {
                    it.addToBackStack(null)
                }
                it.commit()
            }
        }
    }

    private fun menuIdForItem(itemId: Int): Int = when (itemId) {
        R.id.nav_channels -> MENU_CHANNELS
        R.id.nav_program_guide -> MENU_PROGRAM_GUIDE
        R.id.nav_completed_recordings -> MENU_COMPLETED_RECORDINGS
        R.id.nav_scheduled_recordings -> MENU_SCHEDULED_RECORDINGS
        R.id.nav_series_recordings -> MENU_SERIES_RECORDINGS
        R.id.nav_timer_recordings -> MENU_TIMER_RECORDINGS
        R.id.nav_failed_recordings -> MENU_FAILED_RECORDINGS
        R.id.nav_removed_recordings -> MENU_REMOVED_RECORDINGS
        R.id.nav_status -> MENU_STATUS
        R.id.nav_settings -> MENU_SETTINGS
        R.id.nav_extras -> MENU_UNLOCKER
        R.id.nav_help -> MENU_HELP
        else -> MENU_CHANNELS
    }

    private fun itemIdForMenu(menuId: Int): Int = when (menuId) {
        MENU_CHANNELS -> R.id.nav_channels
        MENU_PROGRAM_GUIDE -> R.id.nav_program_guide
        MENU_COMPLETED_RECORDINGS -> R.id.nav_completed_recordings
        MENU_SCHEDULED_RECORDINGS -> R.id.nav_scheduled_recordings
        MENU_SERIES_RECORDINGS -> R.id.nav_series_recordings
        MENU_TIMER_RECORDINGS -> R.id.nav_timer_recordings
        MENU_FAILED_RECORDINGS -> R.id.nav_failed_recordings
        MENU_REMOVED_RECORDINGS -> R.id.nav_removed_recordings
        MENU_STATUS -> R.id.nav_status
        MENU_SETTINGS -> R.id.nav_settings
        MENU_UNLOCKER -> R.id.nav_extras
        MENU_HELP -> R.id.nav_help
        else -> R.id.nav_channels
    }

    companion object {

        // The index for the navigation drawer menus
        const val MENU_CHANNELS = 0
        const val MENU_PROGRAM_GUIDE = 1
        const val MENU_COMPLETED_RECORDINGS = 2
        const val MENU_SCHEDULED_RECORDINGS = 3
        const val MENU_SERIES_RECORDINGS = 4
        const val MENU_TIMER_RECORDINGS = 5
        const val MENU_FAILED_RECORDINGS = 6
        const val MENU_REMOVED_RECORDINGS = 7
        const val MENU_STATUS = 8
        const val MENU_SETTINGS = 9
        const val MENU_UNLOCKER = 10
        const val MENU_HELP = 11
    }
}
