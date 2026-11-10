package org.tvheadend.tvhclient.ui.features.navigation

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.util.getThemeId
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Wraps the [NavigationView] based navigation drawer. Replaces the former MaterialDrawer
 * implementation with the framework/Material Components drawer.
 *
 * The list of destinations, their menu items and their fragments all live in [NavDestination], so
 * this class only has to translate between the drawer state and that single model.
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

    private val connectionRow: TextView =
            navigationView.getHeaderView(0).findViewById(R.id.navigation_header_connection)

    init {
        toggle.syncState()
        toggle.setToolbarNavigationClickListener { activity.onBackPressedDispatcher.onBackPressed() }

        navigationView.getHeaderView(0).setBackgroundResource(
                if (getThemeId(activity) == R.style.CustomTheme_Light) R.drawable.header_light else R.drawable.header_dark)

        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(navigationView)
            NavDestination.fromMenuItemId(item.itemId)?.let { navigationViewModel.setNavigationMenuId(it) }
            true
        }

        connectionRow.setOnClickListener { showConnectionPicker() }

        navigationViewModel.isUnlockedLiveData.observe(activity) { unlocked ->
            navigationView.menu.findItem(R.id.nav_extras)?.isVisible = !unlocked
        }
        navigationViewModel.connectionLiveData.observe(activity) {
            updateConnectionRow()
        }
        navigationViewModel.connections.observe(activity) {
            updateConnectionRow()
        }

        statusViewModel.channelCount.observe(activity) { setBadge(R.id.nav_channels, it) }
        statusViewModel.seriesRecordingCount.observe(activity) { setBadge(R.id.nav_series_recordings, it) }
        statusViewModel.timerRecordingCount.observe(activity) { setBadge(R.id.nav_timer_recordings, it) }
        statusViewModel.completedRecordingCount.observe(activity) { setBadge(R.id.nav_completed_recordings, it) }
        statusViewModel.scheduledRecordingCount.observe(activity) { setBadge(R.id.nav_scheduled_recordings, it) }
        statusViewModel.failedRecordingCount.observe(activity) { setBadge(R.id.nav_failed_recordings, it) }
        statusViewModel.removedRecordingCount.observe(activity) { setBadge(R.id.nav_removed_recordings, it) }

        updateConnectionRow()
    }

    /**
     * Shows the count in the item's trailing action view (see navigation_drawer_count.xml).
     *
     * `NavigationView` has no native badge API (`getOrCreateBadge`/`removeBadge` only exist on
     * `NavigationBarView`), so a custom action view is used instead of mutating the item title.
     */
    private fun setBadge(itemId: Int, count: Int) {
        val countView = (navigationView.menu.findItem(itemId)?.actionView as? TextView) ?: return
        countView.text = if (count > 0) count.toString() else null
        countView.isVisible = count > 0
    }

    private fun updateConnectionRow() {
        val activeConnection = navigationViewModel.connectionLiveData.value
        connectionRow.text = activeConnection?.name ?: activity.getString(R.string.no_connection_available)
    }

    private fun activeConnectionId(): Int = navigationViewModel.connectionLiveData.value?.id ?: -1

    /**
     * Shows the list of available connections and switches to the selected one after a confirmation.
     * Replaces the former header [android.widget.Spinner], whose adapter used to be rebuilt whenever
     * the connection list changed and therefore fired spurious selection callbacks.
     */
    private fun showConnectionPicker() {
        val connections = navigationViewModel.connections.value ?: emptyList()
        if (connections.isEmpty()) {
            return
        }
        val activeId = activeConnectionId()
        val names = connections.map { it.name ?: "" }.toTypedArray()
        val checkedIndex = connections.indexOfFirst { it.id == activeId }

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.pref_connections)
                .setSingleChoiceItems(names, checkedIndex) { dialog, which ->
                    dialog.dismiss()
                    val selectedId = connections[which].id
                    if (selectedId != activeId) {
                        confirmConnectionSwitch(selectedId)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun confirmConnectionSwitch(selectedId: Int) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.connect_to_new_server)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.connect) { _, _ ->
                    activity.lifecycleScope.launch {
                        if (navigationViewModel.setSelectedConnectionAsActive(selectedId)) {
                            navigationViewModel.updateConnectionAndRebind(activity)
                        }
                    }
                }
                .setCancelable(false)
                .show()
    }

    fun getSelectedMenu(): NavDestination =
            navigationView.checkedItem?.let { NavDestination.fromMenuItemId(it.itemId) } ?: NavDestination.CHANNELS

    fun saveInstanceState(outState: Bundle): Bundle {
        // The DrawerLayout and NavigationView persist their own state through their view ids.
        return outState
    }

    fun enableDrawerIndicator(isEnabled: Boolean) {
        toggle.isDrawerIndicatorEnabled = isEnabled
    }

    fun setSelectedNavigationDrawerMenuFromFragmentType(fragment: Fragment?) {
        val destination = NavDestination.fromFragment(fragment) ?: return
        navigationView.setCheckedItem(destination.menuItemId)
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param destination The destination that was selected
     */
    fun handleDrawerItemSelected(destination: NavDestination) {
        Timber.d("Handling new navigation destination $destination")

        // Settings opens a separate Activity instead of a fragment.
        if (destination == NavDestination.SETTINGS) {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
            return
        }

        val fragment = destination.createFragment() ?: return

        // A new or existing main fragment shall be shown. So save the menu position so we
        // know which one was selected. Additionally remove any old details fragment in case
        // dual pane mode is active to prevent showing wrong details data.
        // Finally show the new main fragment and add it to the back stack. Navigation history is
        // always recorded so that Back behaves the same way regardless of any preference.
        if (isDualPane) {
            val detailsFragment = activity.supportFragmentManager.findFragmentById(R.id.details)
            if (detailsFragment != null) {
                activity.supportFragmentManager.beginTransaction().remove(detailsFragment).commit()
            }
        }
        activity.supportFragmentManager.beginTransaction()
                .replace(R.id.main, fragment)
                .addToBackStack(null)
                .commit()
    }
}
