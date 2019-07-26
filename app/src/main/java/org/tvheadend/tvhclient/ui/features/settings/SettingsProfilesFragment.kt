package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

class SettingsProfilesFragment : BasePreferenceFragment() {

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

        addProfileValuesToListPreference(htspPlaybackProfilesPreference, settingsViewModel.getHtspProfiles(), settingsViewModel.currentServerStatus.htspPlaybackServerProfileId)
        addProfileValuesToListPreference(httpPlaybackProfilesPreference, settingsViewModel.getHttpProfiles(), settingsViewModel.currentServerStatus.httpPlaybackServerProfileId)
        addProfileValuesToListPreference(recordingProfilesPreference, settingsViewModel.getRecordingProfiles(), settingsViewModel.currentServerStatus.recordingServerProfileId)
        addProfileValuesToListPreference(castingProfilesPreference, settingsViewModel.getHttpProfiles(), settingsViewModel.currentServerStatus.castingServerProfileId)

        setHttpPlaybackPreferenceSummary()
        setHtspPlaybackPreferenceSummary()
        setRecordingPreferenceSummary()
        setCastingPreferenceSummary()

        settingsViewModel.isUnlocked.observe(viewLifecycleOwner, Observer {
            initProfileChangeListeners()
        })
    }

    private fun initProfileChangeListeners() {
        htspPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.htspPlaybackServerProfileId = Integer.valueOf(o as String)
                setHtspPlaybackPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        httpPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.httpPlaybackServerProfileId = Integer.valueOf(o as String)
                setHttpPlaybackPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        recordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.recordingServerProfileId = Integer.valueOf(o as String)
                setRecordingPreferenceSummary()
                settingsViewModel.updateServerStatus(it)
            }
            true
        }

        if (isUnlocked) {
            castingProfilesPreference.onPreferenceClickListener = null
            castingProfilesPreference.setOnPreferenceChangeListener { _, o ->
                settingsViewModel.currentServerStatus.let {
                    it.castingServerProfileId = Integer.valueOf(o as String)
                    setCastingPreferenceSummary()
                    settingsViewModel.updateServerStatus(it)
                }
                true
            }
        } else {
            castingProfilesPreference.onPreferenceChangeListener = null
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
        if (settingsViewModel.currentServerStatus.htspPlaybackServerProfileId == 0) {
            htspPlaybackProfilesPreference.summary = "None"
        } else {
            htspPlaybackProfilesPreference.summary = settingsViewModel.getHtspProfile()?.name
        }
    }

    private fun setHttpPlaybackPreferenceSummary() {
        if (settingsViewModel.currentServerStatus.httpPlaybackServerProfileId == 0) {
            httpPlaybackProfilesPreference.summary = "None"
        } else {
            httpPlaybackProfilesPreference.summary = settingsViewModel.getHttpProfile()?.name
        }
    }

    private fun setRecordingPreferenceSummary() {
        if (settingsViewModel.currentServerStatus.recordingServerProfileId == 0) {
            recordingProfilesPreference.summary = "None"
        } else {
            recordingProfilesPreference.summary = settingsViewModel.getRecordingProfile()?.name
        }
    }

    private fun setCastingPreferenceSummary() {
        if (settingsViewModel.currentServerStatus.castingServerProfileId == 0) {
            castingProfilesPreference.summary = "None"
        } else {
            castingProfilesPreference.summary = settingsViewModel.getCastingProfile()?.name
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
