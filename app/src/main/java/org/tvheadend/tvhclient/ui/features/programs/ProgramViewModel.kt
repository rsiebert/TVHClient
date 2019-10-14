package org.tvheadend.tvhclient.ui.features.programs

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber

class ProgramViewModel(application: Application) : BaseViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    var selectedTime: Long = System.currentTimeMillis()
    var eventId: Int = 0
    var channelId: Int = 0
    var channelName: String = ""
    val recordings: LiveData<List<Recording>>? = appRepository.recordingData.getLiveDataItems()
    var showProgramChannelIcon: Boolean = false
    var showGenreColor: MutableLiveData<Boolean> = MutableLiveData()
    var showProgramSubtitles: MutableLiveData<Boolean> = MutableLiveData()
    var showProgramArtwork: MutableLiveData<Boolean> = MutableLiveData()

    init {
        Timber.d("Initializing")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_programs_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_artwork_enabled")

        Timber.d("Registering shared preference change listener")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        Timber.d("Unregistering shared preference change listener")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "genre_colors_for_programs_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled))
            "program_subtitle_enabled" -> showProgramSubtitles.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled))
            "program_artwork_enabled" -> showProgramArtwork.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_program_artwork_enabled))
        }
    }

    fun getProgramsFromCurrentChannelFromTime(): LiveData<List<Program>> {
        return appRepository.programData.getLiveDataItemByChannelIdAndTime(channelId, selectedTime)
    }

    fun getProgramsFromTime(): LiveData<List<SearchResultProgram>> {
        return appRepository.programData.getLiveDataItemsFromTime(selectedTime)
    }

    fun getCurrentProgram(): Program? {
        return appRepository.programData.getItemById(eventId)
    }

    fun getRecordingsFromCurrentChannel(): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }
}
