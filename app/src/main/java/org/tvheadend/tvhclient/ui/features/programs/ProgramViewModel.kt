package org.tvheadend.tvhclient.ui.features.programs

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import timber.log.Timber
import javax.inject.Inject

class ProgramViewModel() : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val recordings: LiveData<List<Recording>>?
    var showProgramChannelIcon: Boolean = false
    var showGenreColor: MutableLiveData<Boolean> = MutableLiveData()
    var showProgramSubtitles: MutableLiveData<Boolean> = MutableLiveData()
    var showProgramArtwork: MutableLiveData<Boolean> = MutableLiveData()

    init {
        Timber.d("Initializing")
        MainApplication.getComponent().inject(this)

        recordings = appRepository.recordingData.getLiveDataItems()

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
            "genre_colors_for_programs_enabled" -> showGenreColor.value = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", appContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled))
            "program_subtitle_enabled" -> showProgramSubtitles.value = sharedPreferences.getBoolean("program_subtitle_enabled", appContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled))
            "program_artwork_enabled" -> showProgramArtwork.value = sharedPreferences.getBoolean("program_artwork_enabled", appContext.resources.getBoolean(R.bool.pref_default_program_artwork_enabled))
        }
    }

    fun getProgramsByChannelFromTime(channelId: Int, time: Long): LiveData<List<Program>> {
        return appRepository.programData.getLiveDataItemByChannelIdAndTime(channelId, time)
    }

    fun getProgramsFromTime(time: Long): LiveData<List<SearchResultProgram>> {
        return appRepository.programData.getLiveDataItemsFromTime(time)
    }

    fun getProgramByIdSync(eventId: Int): Program? {
        return appRepository.programData.getItemById(eventId)
    }

    fun getRecordingsByChannelId(channelId: Int): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }
}
