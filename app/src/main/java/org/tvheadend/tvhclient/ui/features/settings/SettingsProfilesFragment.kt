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
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage

class SettingsProfilesFragment : BasePreferenceFragment() {

    private lateinit var currentServerStatus: ServerStatus
    private lateinit var recordingProfilesPreference: ListPreference
    private lateinit var htspPlaybackProfilesPreference: ListPreference
    private lateinit var httpPlaybackProfilesPreference: ListPreference
    private lateinit var castingProfilesPreference: ListPreference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_profiles))
        toolbarInterface.setSubtitle(settingsViewModel.connection.name ?: "")

        htspPlaybackProfilesPreference = findPreference("htsp_playback_profiles")!!
        httpPlaybackProfilesPreference = findPreference("http_playback_profiles")!!
        recordingProfilesPreference = findPreference("recording_profiles")!!
        castingProfilesPreference = findPreference("casting_profiles")!!

        settingsViewModel.serverStatusLiveData.observe(viewLifecycleOwner, Observer { serverStatus ->
            currentServerStatus = serverStatus

            addProfiles(htspPlaybackProfilesPreference, settingsViewModel.getHtspProfiles(), currentServerStatus.htspPlaybackServerProfileId)
            addProfiles(httpPlaybackProfilesPreference, settingsViewModel.getHttpProfiles(), currentServerStatus.httpPlaybackServerProfileId)
            addProfiles(recordingProfilesPreference, settingsViewModel.getRecordingProfiles(), currentServerStatus.recordingServerProfileId)
            addProfiles(castingProfilesPreference, settingsViewModel.getHttpProfiles(), currentServerStatus.castingServerProfileId)

            setHttpPlaybackProfileListSummary()
            setHtspPlaybackProfileListSummary()
            setRecordingProfileListSummary()
            setCastingProfileListSummary()

            htspPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
                currentServerStatus.let {
                    it.htspPlaybackServerProfileId = Integer.valueOf(o as String)
                    setHtspPlaybackProfileListSummary()
                    settingsViewModel.updateServerStatus(it)
                }
                true
            }
            httpPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
                currentServerStatus.let {
                    it.httpPlaybackServerProfileId = Integer.valueOf(o as String)
                    setHttpPlaybackProfileListSummary()
                    settingsViewModel.updateServerStatus(it)
                }
                true
            }
            recordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
                currentServerStatus.let {
                    it.recordingServerProfileId = Integer.valueOf(o as String)
                    setRecordingProfileListSummary()
                    settingsViewModel.updateServerStatus(it)
                }
                true
            }

            if (isUnlocked) {
                castingProfilesPreference.setOnPreferenceChangeListener { _, o ->
                    currentServerStatus.let {
                        it.castingServerProfileId = Integer.valueOf(o as String)
                        setCastingProfileListSummary()
                        settingsViewModel.updateServerStatus(it)
                    }
                    true
                }
            } else {
                castingProfilesPreference.setOnPreferenceClickListener {
                    context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
                    true
                }
            }
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_profiles, rootKey)
    }

    private fun setHtspPlaybackProfileListSummary() {
        if (currentServerStatus.htspPlaybackServerProfileId == 0) {
            htspPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(currentServerStatus.htspPlaybackServerProfileId)
            htspPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setHttpPlaybackProfileListSummary() {
        if (currentServerStatus.httpPlaybackServerProfileId == 0) {
            httpPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(currentServerStatus.httpPlaybackServerProfileId)
            httpPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setRecordingProfileListSummary() {
        if (currentServerStatus.recordingServerProfileId == 0) {
            recordingProfilesPreference.summary = "None"
        } else {
            val recordingProfile = settingsViewModel.getProfileById(currentServerStatus.recordingServerProfileId)
            recordingProfilesPreference.summary = recordingProfile?.name
        }
    }

    private fun setCastingProfileListSummary() {
        if (currentServerStatus.castingServerProfileId == 0) {
            castingProfilesPreference.summary = "None"
        } else {
            val castingProfile = settingsViewModel.getProfileById(currentServerStatus.castingServerProfileId)
            castingProfilesPreference.summary = castingProfile?.name
        }
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
