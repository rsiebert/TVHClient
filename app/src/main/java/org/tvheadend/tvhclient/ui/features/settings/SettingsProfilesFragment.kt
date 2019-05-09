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

import android.os.Bundle
import androidx.preference.ListPreference
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage

// TODO use view model to store the selected ids

class SettingsProfilesFragment : BasePreferenceFragment(), BackPressedInterface {

    private lateinit var recordingProfilesPreference: ListPreference
    private lateinit var htspPlaybackProfilesPreference: ListPreference
    private lateinit var httpPlaybackProfilesPreference: ListPreference
    private lateinit var castingProfilesPreference: ListPreference
    private var htspPlaybackServerProfileId: Int = 0
    private var httpPlaybackServerProfileId: Int = 0
    private var recordingServerProfileId: Int = 0
    private var castingServerProfileId: Int = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_profiles))
        toolbarInterface.setSubtitle(settingsViewModel.connection.name ?: "")

        htspPlaybackProfilesPreference = findPreference("htsp_playback_profiles")!!
        httpPlaybackProfilesPreference = findPreference("http_playback_profiles")!!
        recordingProfilesPreference = findPreference("recording_profiles")!!
        castingProfilesPreference = findPreference("casting_profiles")!!

        if (savedInstanceState != null) {
            htspPlaybackServerProfileId = savedInstanceState.getInt("htsp_playback_profile_id")
            httpPlaybackServerProfileId = savedInstanceState.getInt("http_playback_profile_id")
            recordingServerProfileId = savedInstanceState.getInt("recording_profile_id")
            castingServerProfileId = savedInstanceState.getInt("casting_profile_id")
        } else {
            htspPlaybackServerProfileId = serverStatus.htspPlaybackServerProfileId
            httpPlaybackServerProfileId = serverStatus.httpPlaybackServerProfileId
            recordingServerProfileId = serverStatus.recordingServerProfileId
            castingServerProfileId = serverStatus.castingServerProfileId
        }

        addProfiles(htspPlaybackProfilesPreference, settingsViewModel.getHtspProfiles(), htspPlaybackServerProfileId)
        addProfiles(httpPlaybackProfilesPreference, settingsViewModel.getHttpProfiles(), httpPlaybackServerProfileId)
        addProfiles(recordingProfilesPreference, settingsViewModel.getRecordingProfiles(), recordingServerProfileId)
        addProfiles(castingProfilesPreference, settingsViewModel.getHttpProfiles(), castingServerProfileId)

        setHttpPlaybackProfileListSummary()
        setHtspPlaybackProfileListSummary()
        setRecordingProfileListSummary()
        setCastingProfileListSummary()

        htspPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            htspPlaybackServerProfileId = Integer.valueOf(o as String)
            setHtspPlaybackProfileListSummary()
            true
        }
        httpPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            httpPlaybackServerProfileId = Integer.valueOf(o as String)
            setHttpPlaybackProfileListSummary()
            true
        }
        recordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            recordingServerProfileId = Integer.valueOf(o as String)
            setRecordingProfileListSummary()
            true
        }

        if (isUnlocked) {
            castingProfilesPreference.setOnPreferenceChangeListener { _, o ->
                castingServerProfileId = Integer.valueOf(o as String)
                setCastingProfileListSummary()
                true
            }
        } else {
            castingProfilesPreference.setOnPreferenceClickListener {
                context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
                true
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_profiles, rootKey)
    }

    private fun setHtspPlaybackProfileListSummary() {
        if (htspPlaybackServerProfileId == 0) {
            htspPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(htspPlaybackServerProfileId)
            htspPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setHttpPlaybackProfileListSummary() {
        if (httpPlaybackServerProfileId == 0) {
            httpPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(httpPlaybackServerProfileId)
            httpPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setRecordingProfileListSummary() {
        if (recordingServerProfileId == 0) {
            recordingProfilesPreference.summary = "None"
        } else {
            val recordingProfile = settingsViewModel.getProfileById(recordingServerProfileId)
            recordingProfilesPreference.summary = recordingProfile?.name
        }
    }

    private fun setCastingProfileListSummary() {
        if (castingServerProfileId == 0) {
            castingProfilesPreference.summary = "None"
        } else {
            val castingProfile = settingsViewModel.getProfileById(castingServerProfileId)
            castingProfilesPreference.summary = castingProfile?.name
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("htsp_playback_profile_id", htspPlaybackServerProfileId)
        outState.putInt("http_playback_profile_id", httpPlaybackServerProfileId)
        outState.putInt("recording_profile_id", recordingServerProfileId)
        outState.putInt("casting_profile_id", castingServerProfileId)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        serverStatus.htspPlaybackServerProfileId = htspPlaybackServerProfileId
        serverStatus.httpPlaybackServerProfileId = httpPlaybackServerProfileId
        serverStatus.recordingServerProfileId = recordingServerProfileId
        if (isUnlocked) {
            serverStatus.castingServerProfileId = castingServerProfileId
        }
        settingsViewModel.updateServerStatus(serverStatus)
        activity?.finish()
    }

    private fun addProfiles(listPreference: ListPreference?, serverProfileList: List<ServerProfile>, selectedIndex: Int) {
        // Initialize the arrays that contain the profile values
        val size = serverProfileList.size + 1
        val entries = arrayOfNulls<CharSequence>(size)
        val entryValues = arrayOfNulls<CharSequence>(size)

        entries[0] = "None"
        entryValues[0] = "0"

        // Add the available profiles to list preference
        var index = 0
        for (i in 1 until size) {
            val (id, _, _, name) = serverProfileList[i - 1]
            entries[i] = name
            entryValues[i] = id.toString()
            if (selectedIndex == id) {
                index = i
            }
        }
        listPreference?.entries = entries
        listPreference?.entryValues = entryValues
        listPreference?.setValueIndex(index)
    }
}
