/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage
import timber.log.Timber

class SettingsUserInterfaceFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private var programArtworkEnabledPreference: CheckBoxPreference? = null
    private var castMiniControllerPreference: CheckBoxPreference? = null
    private var multipleChannelTagsPreference: CheckBoxPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_user_interface))
        toolbarInterface.setSubtitle("")

        programArtworkEnabledPreference = findPreference("program_artwork_enabled")
        programArtworkEnabledPreference?.onPreferenceClickListener = this
        castMiniControllerPreference = findPreference("casting_minicontroller_enabled")
        castMiniControllerPreference?.onPreferenceClickListener = this
        multipleChannelTagsPreference = findPreference("multiple_channel_tags_enabled")
        multipleChannelTagsPreference?.onPreferenceClickListener = this
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_ui)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "multiple_channel_tags_enabled" -> handlePreferenceMultipleChannelTagsSelected()
            "program_artwork_enabled" -> handlePreferenceShowArtworkSelected()
            "casting" -> handlePreferenceCastingSelected()
        }
        return true
    }

    private fun handlePreferenceMultipleChannelTagsSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            multipleChannelTagsPreference?.isChecked = false
        }
    }

    private fun handlePreferenceShowArtworkSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            programArtworkEnabledPreference?.isChecked = false
        }
    }

    private fun handlePreferenceCastingSelected() {
        if (htspVersion < 16) {
            context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
            castMiniControllerPreference?.isChecked = false
        } else if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            castMiniControllerPreference?.isChecked = false
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        Timber.d("Preference $key has changed")
        when (key) {
            "hours_of_epg_data_per_screen" ->
                try {
                    val value = Integer.parseInt(prefs.getString(key, resources.getString(R.string.pref_default_hours_of_epg_data_per_screen))!!)
                    if (value < 1) {
                        (findPreference<Preference>(key) as EditTextPreference).text = "1"
                        prefs.edit().putString(key, "1").apply()
                    }
                    if (value > 24) {
                        (findPreference<Preference>(key) as EditTextPreference).text = "24"
                        prefs.edit().putString(key, "24").apply()
                    }
                } catch (ex: NumberFormatException) {
                    prefs.edit().putString(key, resources.getString(R.string.pref_default_hours_of_epg_data_per_screen)).apply()
                }

            "days_of_epg_data" ->
                try {
                    val value = Integer.parseInt(prefs.getString(key, resources.getString(R.string.pref_default_days_of_epg_data))!!)
                    if (value < 1) {
                        (findPreference<Preference>(key) as EditTextPreference).text = "1"
                        prefs.edit().putString(key, "1").apply()
                    }
                    if (value > 14) {
                        (findPreference<Preference>(key) as EditTextPreference).text = "24"
                        prefs.edit().putString(key, "24").apply()
                    }
                } catch (ex: NumberFormatException) {
                    prefs.edit().putString(key, resources.getString(R.string.pref_default_days_of_epg_data)).apply()
                }
        }
    }
}
