package org.tvheadend.tvhclient.ui.features.programs

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber
import java.util.*

class ProgramViewModel(application: Application) : BaseViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    var program = MediatorLiveData<Program>()
    var programs: LiveData<List<Program>>
    var recordings: LiveData<List<Recording>>

    var selectedTime: Long = System.currentTimeMillis()
    var eventId = 0
    var channelId = 0
    var channelName = ""

    var showProgramChannelIcon = false
    var showGenreColor = MutableLiveData<Boolean>()
    var showProgramSubtitles = MutableLiveData<Boolean>()
    var showProgramArtwork = MutableLiveData<Boolean>()

    val eventIdLiveData = MutableLiveData(0)
    val channelIdLiveData = MutableLiveData(0)
    val selectedTimeLiveData = MutableLiveData(Date().time)

    private val defaultShowGenreColor = application.applicationContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_programs_enabled)
    private val defaultShowProgramSubtitles = application.applicationContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled)
    private val defaultShowProgramArtwork = application.applicationContext.resources.getBoolean(R.bool.pref_default_program_artwork_enabled)

    init {
        Timber.d("Initializing")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_programs_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_artwork_enabled")

        program.addSource(eventIdLiveData) { value ->
            if (value > 0) {
                program.value = appRepository.programData.getItemById(value)
            }
        }

        programs = ProgramLiveData(channelIdLiveData, selectedTimeLiveData).switchMap { value ->
            val channelId = value.first ?: 0
            val selectedTime = value.second ?: Date().time

            if (channelId == 0) {
                return@switchMap appRepository.programData.getLiveDataItemsFromTime(selectedTime)
            } else {
                return@switchMap appRepository.programData.getLiveDataItemByChannelIdAndTime(channelId, selectedTime)
            }
        }

        recordings = RecordingLiveData(channelIdLiveData).switchMap { value ->
            val channelId = value ?: 0
            if (channelId == 0) {
                return@switchMap appRepository.recordingData.getLiveDataItems()
            } else {
                return@switchMap appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
            }
        }

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
            "genre_colors_for_programs_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, defaultShowGenreColor)
            "program_subtitle_enabled" -> showProgramSubtitles.value = sharedPreferences.getBoolean(key, defaultShowProgramSubtitles)
            "program_artwork_enabled" -> showProgramArtwork.value = sharedPreferences.getBoolean(key, defaultShowProgramArtwork)
        }
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    internal class ProgramLiveData(channelId: LiveData<Int>,
                                         selectedTime: LiveData<Long>) : MediatorLiveData<Pair<Int?, Long?>>() {
        init {
            addSource(channelId) { id ->
                value = Pair(id, selectedTime.value)
            }
            addSource(selectedTime) { time ->
                value = Pair(channelId.value, time)
            }
        }
    }

    internal inner class RecordingLiveData(channelId: LiveData<Int>) : MediatorLiveData<Int?>() {
        init {
            addSource(channelId) { id ->
                value = id
            }
        }
    }
}
