package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsProfilesFragment : PreferenceFragmentCompat() {

    private lateinit var recordingProfilesPreference: ListPreference
    private lateinit var seriesRecordingProfilesPreference: ListPreference
    private lateinit var timerRecordingProfilesPreference: ListPreference
    private lateinit var htspPlaybackProfilesPreference: ListPreference
    private lateinit var httpPlaybackProfilesPreference: ListPreference
    private lateinit var castingProfilesPreference: ListPreference
    lateinit var settingsViewModel: SettingsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingsViewModel = ViewModelProvider(activity as SettingsActivity).get(SettingsViewModel::class.java)

        val toolbarInterface = (activity as ToolbarInterface)
        toolbarInterface.setTitle(getString(R.string.pref_profiles))

        htspPlaybackProfilesPreference = findPreference("htsp_playback_profiles")!!
        httpPlaybackProfilesPreference = findPreference("http_playback_profiles")!!
        recordingProfilesPreference = findPreference("recording_profiles")!!
        seriesRecordingProfilesPreference = findPreference("series_recording_profiles")!!
        timerRecordingProfilesPreference = findPreference("timer_recording_profiles")!!
        castingProfilesPreference = findPreference("casting_profiles")!!

        addProfileValuesToListPreference(htspPlaybackProfilesPreference, settingsViewModel.getHtspProfiles(), settingsViewModel.currentServerStatus.htspPlaybackServerProfileId)
        addProfileValuesToListPreference(httpPlaybackProfilesPreference, settingsViewModel.getHttpProfiles(), settingsViewModel.currentServerStatus.httpPlaybackServerProfileId)
        addProfileValuesToListPreference(recordingProfilesPreference, settingsViewModel.getRecordingProfiles(), settingsViewModel.currentServerStatus.recordingServerProfileId)
        addProfileValuesToListPreference(seriesRecordingProfilesPreference, settingsViewModel.getRecordingProfiles(), settingsViewModel.currentServerStatus.seriesRecordingServerProfileId)
        addProfileValuesToListPreference(timerRecordingProfilesPreference, settingsViewModel.getRecordingProfiles(), settingsViewModel.currentServerStatus.timerRecordingServerProfileId)
        addProfileValuesToListPreference(castingProfilesPreference, settingsViewModel.getHttpProfiles(), settingsViewModel.currentServerStatus.castingServerProfileId)

        settingsViewModel.activeConnectionLiveData.observe(viewLifecycleOwner, Observer { connection ->
            toolbarInterface.setSubtitle(connection.name ?: "")
        })

        settingsViewModel.isUnlockedLiveData.observe(viewLifecycleOwner, Observer {
            initProfileChangeListeners()
        })

        settingsViewModel.currentServerStatusLiveData.observe(viewLifecycleOwner, Observer { _ ->
            setHttpPlaybackPreferenceSummary()
            setHtspPlaybackPreferenceSummary()
            setRecordingPreferenceSummary()
            setSeriesRecordingPreferenceSummary()
            setTimerRecordingPreferenceSummary()
            setCastingPreferenceSummary()
        })
    }

    private fun initProfileChangeListeners() {
        htspPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.htspPlaybackServerProfileId = Integer.valueOf(o as String)
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        httpPlaybackProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.httpPlaybackServerProfileId = Integer.valueOf(o as String)
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        recordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.recordingServerProfileId = Integer.valueOf(o as String)
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        seriesRecordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.seriesRecordingServerProfileId = Integer.valueOf(o as String)
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        timerRecordingProfilesPreference.setOnPreferenceChangeListener { _, o ->
            settingsViewModel.currentServerStatus.let {
                it.timerRecordingServerProfileId = Integer.valueOf(o as String)
                settingsViewModel.updateServerStatus(it)
            }
            true
        }
        if (settingsViewModel.isUnlocked) {
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
        Timber.d("Htsp playback profile id is ${settingsViewModel.currentServerStatus.htspPlaybackServerProfileId}")
        if (settingsViewModel.currentServerStatus.htspPlaybackServerProfileId == 0) {
            htspPlaybackProfilesPreference.summary = "None"
        } else {
            htspPlaybackProfilesPreference.summary = settingsViewModel.getHtspProfile()?.name
        }
    }

    private fun setHttpPlaybackPreferenceSummary() {
        Timber.d("Http playback profile id is ${settingsViewModel.currentServerStatus.httpPlaybackServerProfileId}")
        if (settingsViewModel.currentServerStatus.httpPlaybackServerProfileId == 0) {
            httpPlaybackProfilesPreference.summary = "None"
        } else {
            httpPlaybackProfilesPreference.summary = settingsViewModel.getHttpProfile()?.name
        }
    }

    private fun setRecordingPreferenceSummary() {
        Timber.d("Recording profile id is ${settingsViewModel.currentServerStatus.recordingServerProfileId}")
        if (settingsViewModel.currentServerStatus.recordingServerProfileId == 0) {
            recordingProfilesPreference.summary = "None"
        } else {
            recordingProfilesPreference.summary = settingsViewModel.getRecordingProfile()?.name
        }
    }

    private fun setSeriesRecordingPreferenceSummary() {
        Timber.d("Series recording profile id is ${settingsViewModel.currentServerStatus.seriesRecordingServerProfileId}")
        if (settingsViewModel.currentServerStatus.seriesRecordingServerProfileId == 0) {
            seriesRecordingProfilesPreference.summary = "None"
        } else {
            seriesRecordingProfilesPreference.summary = settingsViewModel.getSeriesRecordingProfile()?.name
        }
    }

    private fun setTimerRecordingPreferenceSummary() {
        Timber.d("Timer recording profile id is ${settingsViewModel.currentServerStatus.timerRecordingServerProfileId}")
        if (settingsViewModel.currentServerStatus.timerRecordingServerProfileId == 0) {
            timerRecordingProfilesPreference.summary = "None"
        } else {
            timerRecordingProfilesPreference.summary = settingsViewModel.getTimerRecordingProfile()?.name
        }
    }

    private fun setCastingPreferenceSummary() {
        Timber.d("Casting profile id is ${settingsViewModel.currentServerStatus.recordingServerProfileId}")
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
