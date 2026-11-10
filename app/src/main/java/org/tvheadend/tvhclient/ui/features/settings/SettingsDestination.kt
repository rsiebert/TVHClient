package org.tvheadend.tvhclient.ui.features.settings

import androidx.fragment.app.Fragment
import org.tvheadend.tvhclient.ui.features.information.ChangeLogFragment
import org.tvheadend.tvhclient.ui.features.information.InformationFragment
import org.tvheadend.tvhclient.ui.features.information.PrivacyPolicyFragment
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment

/**
 * A screen that can be shown inside [SettingsActivity].
 *
 * Replaces the ad-hoc string keys that used to travel around as `setting_type` intent extras and as
 * `Preference` keys. The [id] keeps the same value so existing `setting_type` extras and preference
 * keys keep working, but callers pass a type instead of a raw string and [fragmentFactory] is the
 * single place that knows which fragment a destination shows.
 */
enum class SettingsDestination(val id: String, val fragmentFactory: () -> Fragment) {
    MAIN("default", { SettingsFragment() }),
    LIST_CONNECTIONS("list_connections", { SettingsListConnectionsFragment() }),
    ADD_CONNECTION("add_connection", { SettingsAddConnectionFragment() }),
    EDIT_CONNECTION("edit_connection", { SettingsEditConnectionFragment() }),
    USER_INTERFACE("user_interface", { SettingsUserInterfaceFragment() }),
    PROFILES("profiles", { SettingsProfilesFragment() }),
    PLAYBACK("playback", { SettingsPlaybackFragment() }),
    ADVANCED("advanced", { SettingsAdvancedFragment() }),
    UNLOCKER("unlocker", { UnlockerFragment() }),
    INFORMATION("information", { InformationFragment() }),
    PRIVACY_POLICY("privacy_policy", { PrivacyPolicyFragment() }),
    CHANGELOG("changelog", { ChangeLogFragment.newInstance(showFullChangelog = true) });

    companion object {

        /** Resolves the destination for a persisted or streamed id, defaulting to [MAIN]. */
        fun fromId(id: String?): SettingsDestination = entries.firstOrNull { it.id == id } ?: MAIN
    }
}
