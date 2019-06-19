package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

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

            addProfileValuesToListPreference(htspPlaybackProfilesPreference, settingsViewModel.getHtspProfiles(), currentServerStatus.htspPlaybackServerProfileId)
            addProfileValuesToListPreference(httpPlaybackProfilesPreference, settingsViewModel.getHttpProfiles(), currentServerStatus.httpPlaybackServerProfileId)
            addProfileValuesToListPreference(recordingProfilesPreference, settingsViewModel.getRecordingProfiles(), currentServerStatus.recordingServerProfileId)
            addProfileValuesToListPreference(castingProfilesPreference, settingsViewModel.getHttpProfiles(), currentServerStatus.castingServerProfileId)

            setHttpPlaybackPreferenceSummary()
            setHtspPlaybackPreferenceSummary()
            setRecordingPreferenceSummary()
            setCastingPreferenceSummary()

            initProfileChangeListeners()
        })
    }

    private fun initProfileChangeListeners() {
        htspPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            currentServerStatus.let {
                it.htspPlaybackServerProfileId = Integer.valueOf(o as String)
                setHtspPlaybackPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        httpPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            currentServerStatus.let {
                it.httpPlaybackServerProfileId = Integer.valueOf(o as String)
                setHttpPlaybackPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        recordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            currentServerStatus.let {
                it.recordingServerProfileId = Integer.valueOf(o as String)
                setRecordingPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }

        if (isUnlocked) {
            castingProfilesPreference.setOnPreferenceChangeListener { _, o ->
                currentServerStatus.let {
                    it.castingServerProfileId = Integer.valueOf(o as String)
                    setCastingPreferenceSummary()
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
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_profiles, rootKey)
    }

    private fun setHtspPlaybackPreferenceSummary() {
        if (currentServerStatus.htspPlaybackServerProfileId == 0) {
            htspPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(currentServerStatus.htspPlaybackServerProfileId)
            htspPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setHttpPlaybackPreferenceSummary() {
        if (currentServerStatus.httpPlaybackServerProfileId == 0) {
            httpPlaybackProfilesPreference.summary = "None"
        } else {
            val playbackProfile = settingsViewModel.getProfileById(currentServerStatus.httpPlaybackServerProfileId)
            httpPlaybackProfilesPreference.summary = playbackProfile?.name
        }
    }

    private fun setRecordingPreferenceSummary() {
        if (currentServerStatus.recordingServerProfileId == 0) {
            recordingProfilesPreference.summary = "None"
        } else {
            val recordingProfile = settingsViewModel.getProfileById(currentServerStatus.recordingServerProfileId)
            recordingProfilesPreference.summary = recordingProfile?.name
        }
    }

    private fun setCastingPreferenceSummary() {
        if (currentServerStatus.castingServerProfileId == 0) {
            castingProfilesPreference.summary = "None"
        } else {
            val castingProfile = settingsViewModel.getProfileById(currentServerStatus.castingServerProfileId)
            castingProfilesPreference.summary = castingProfile?.name
        }
    }

    private fun addProfileValuesToListPreference(listPreference: ListPreference?, serverProfileList: List<ServerProfile>, selectedIndex: Int) {
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
