package org.tvheadend.tvhclient.ui.common

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import org.tvheadend.data.entity.ProgramInterface
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

// Constants required for the date calculation
private const val TWO_DAYS = 1000 * 3600 * 24 * 2
private const val SIX_DAYS = 1000 * 3600 * 24 * 6

@BindingAdapter("marginStart")
fun setLayoutWidth(view: View, increaseMargin: Boolean) {
    val layoutParams = view.layoutParams
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val marginStart = (if (increaseMargin)
            view.context.resources.getDimension(R.dimen.dp_80)
        else
            view.context.resources.getDimension(R.dimen.dp_16)).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.marginStart = marginStart
            view.layoutParams = layoutParams
        } else {
            layoutParams.setMargins(marginStart,
                    layoutParams.topMargin,
                    layoutParams.rightMargin,
                    layoutParams.bottomMargin)
        }
    }
}

@BindingAdapter("layoutWidth")
fun setLayoutWidth(view: View, width: Int) {
    val layoutParams = view.layoutParams as RecyclerView.LayoutParams
    layoutParams.width = width
    view.layoutParams = layoutParams
}

@BindingAdapter("seriesInfoText")
fun setSeriesInfoText(view: TextView, program: ProgramInterface?) {
    val context = view.context
    val season = context.resources.getString(R.string.season)
    val episode = context.resources.getString(R.string.episode)
    val part = context.resources.getString(R.string.part)

    var seriesInfo = ""
    if (program != null) {
        if (!program.episodeOnscreen.isNullOrEmpty()) {
            seriesInfo = program.episodeOnscreen ?: ""
        } else {
            if (program.seasonNumber > 0) {
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        season.toLowerCase(Locale.getDefault()), program.seasonNumber)
            }
            if (program.episodeNumber > 0) {
                if (seriesInfo.isNotEmpty()) {
                    seriesInfo += ", "
                }
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        episode.toLowerCase(Locale.getDefault()), program.episodeNumber)
            }
            if (program.partNumber > 0) {
                if (seriesInfo.isNotEmpty()) {
                    seriesInfo += ", "
                }
                seriesInfo += String.format(Locale.getDefault(), "%s %d",
                        part.toLowerCase(Locale.getDefault()), program.partNumber)
            }
            if (seriesInfo.isNotEmpty()) {
                seriesInfo = seriesInfo.substring(0, 1).toUpperCase(
                        Locale.getDefault()) + seriesInfo.substring(1)
            }
        }
    }
    view.visibleOrGone(seriesInfo.isNotEmpty())
    view.text = seriesInfo
}

@BindingAdapter("contentTypeText")
fun setContentTypeText(view: TextView, contentType: Int) {
    val ret = SparseArray<String>()
    val context = view.context

    var s = context.resources.getStringArray(R.array.pr_content_type0)
    for (i in s.indices) {
        ret.append(i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type1)
    for (i in s.indices) {
        ret.append(0x10 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type2)
    for (i in s.indices) {
        ret.append(0x20 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type3)
    for (i in s.indices) {
        ret.append(0x30 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type4)
    for (i in s.indices) {
        ret.append(0x40 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type5)
    for (i in s.indices) {
        ret.append(0x50 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type6)
    for (i in s.indices) {
        ret.append(0x60 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type7)
    for (i in s.indices) {
        ret.append(0x70 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type8)
    for (i in s.indices) {
        ret.append(0x80 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type9)
    for (i in s.indices) {
        ret.append(0x90 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type10)
    for (i in s.indices) {
        ret.append(0xa0 + i, s[i])
    }
    s = context.resources.getStringArray(R.array.pr_content_type11)
    for (i in s.indices) {
        ret.append(0xb0 + i, s[i])
    }
    val contentTypeText = ret.get(contentType, context.getString(R.string.no_data))
    view.visibleOrGone(contentTypeText.isNotEmpty())
    view.text = contentTypeText
}

@BindingAdapter("priorityText")
fun setPriorityText(view: TextView, priority: Int) {
    val priorityNames = view.context.resources.getStringArray(R.array.dvr_priority_names)
    when (priority) {
        in 0..4 -> view.text = priorityNames[priority]
        6 -> view.text = priorityNames[5]
        else -> view.text = ""
    }
}

@BindingAdapter("dataSizeText")
fun setDataSizeText(view: TextView, recording: Recording?) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.context.resources.getBoolean(R.bool.pref_default_show_recording_file_status_enabled))

    if (showRecordingFileStatus
            && recording != null
            && (!recording.isScheduled || recording.isScheduled && recording.isRecording)) {
        view.visible()
        if (recording.dataSize > 1048576) {
            view.text = context.resources.getString(R.string.data_size, recording.dataSize / 1048576, "MB")
        } else {
            view.text = context.resources.getString(R.string.data_size, recording.dataSize / 1024, "KB")
        }
    } else {
        view.gone()
    }
}

@BindingAdapter("dataErrorText")
fun setDataErrorText(view: TextView, recording: Recording?) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.context.resources.getBoolean(R.bool.pref_default_show_recording_file_status_enabled))

    if (showRecordingFileStatus
            && recording != null
            && !recording.dataErrors.isNullOrEmpty()
            && (!recording.isScheduled || recording.isScheduled && recording.isRecording)) {
        view.visible()
        view.text = context.resources.getString(R.string.data_errors, if (recording.dataErrors == null) "0" else recording.dataErrors)
    } else {
        view.gone()
    }
}

@BindingAdapter("subscriptionErrorText")
fun setSubscriptionErrorText(view: TextView, recording: Recording?) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.context.resources.getBoolean(R.bool.pref_default_show_recording_file_status_enabled))

    if (showRecordingFileStatus
            && recording != null
            && !recording.isScheduled
            && !recording.subscriptionError.isNullOrEmpty()) {
        view.visible()
        view.text = context.resources.getString(R.string.subscription_error, recording.subscriptionError)
    } else {
        view.gone()
    }
}

@BindingAdapter("streamErrorText")
fun setStreamErrorText(view: TextView, recording: Recording?) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.context.resources.getBoolean(R.bool.pref_default_show_recording_file_status_enabled))

    if (showRecordingFileStatus
            && recording != null
            && !recording.isScheduled
            && !recording.streamErrors.isNullOrEmpty()) {
        view.visible()
        view.text = context.resources.getString(R.string.stream_errors, recording.streamErrors)
    } else {
        view.gone()
    }
}

@BindingAdapter("statusLabelVisibility")
fun setStatusLabelVisibility(view: TextView, recording: Recording?) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", context.resources.getBoolean(R.bool.pref_default_show_recording_file_status_enabled))

    if (showRecordingFileStatus
            && recording != null
            && !recording.isScheduled) {
        view.visible()
    } else {
        view.gone()
    }
}

@BindingAdapter("disabledText", "htspVersion")
fun setDisabledText(view: TextView, recording: Recording?, htspVersion: Int) {
    if (recording == null || !recording.isScheduled) {
        view.gone()
    } else {
        setDisabledText(view, recording.isEnabled, htspVersion)
    }
}

@BindingAdapter("disabledText", "htspVersion")
fun setDisabledText(view: TextView, isEnabled: Boolean, htspVersion: Int) {
    view.visibleOrGone(htspVersion >= 19 && !isEnabled)
    view.setText(if (isEnabled) R.string.recording_enabled else R.string.recording_disabled)
}

@BindingAdapter("duplicateText", "htspVersion")
fun setDuplicateText(view: TextView, recording: Recording?, htspVersion: Int) {
    if (recording == null || !recording.isScheduled) {
        view.gone()
    } else {
        view.visibleOrGone(htspVersion >= 33 && recording.duplicate != 0)
        view.setText(R.string.duplicate_recording)
    }
}

@BindingAdapter("failedReasonText")
fun setFailedReasonText(view: TextView, recording: Recording?) {
    val context = view.context
    var failedReasonText = ""

    if (recording != null) {
        when {
            recording.isAborted -> failedReasonText = context.resources.getString(R.string.recording_canceled)
            recording.isMissed -> failedReasonText = context.resources.getString(R.string.recording_time_missed)
            recording.isFailed -> failedReasonText = context.resources.getString(R.string.recording_file_invalid)
            recording.isFileMissing -> failedReasonText = context.resources.getString(R.string.recording_file_missing)
        }
    }

    view.visibleOrGone(failedReasonText.isNotEmpty()
            && recording != null
            && !recording.isCompleted)
    view.text = failedReasonText
}

@BindingAdapter("optionalDescriptionText")
fun setOptionalDescriptionText(view: TextView, text: String?) {
    view.visibleOrGone(!text.isNullOrEmpty())
    if (text.isNullOrEmpty()) return

    if (text.contains("[COLOR ") && text.contains("[/COLOR]")) {
        val builder = SpannableStringBuilder()
        builder.append(text.substringBefore("[COLOR ", ""))

        val textArray = text.split("[COLOR").toTypedArray()
        textArray.forEach { str ->
            val colorName = str.substringBefore("]", "").trim()
            val coloredText = SpannableString(str.substringAfter("]", "").substringBefore("[/COLOR]", ""))
            val remainingText = str.substringAfter("[/COLOR]", "")

            val colorId = view.resources.getIdentifier(colorName, "color", MainApplication.instance.packageName)
            if (colorId > 0) {
                coloredText.setSpan(ForegroundColorSpan(ContextCompat.getColor(view.context, colorId)), 0, coloredText.length, 0)
            }
            builder.append(coloredText)
            builder.append(remainingText)
        }
        view.setText( builder, TextView.BufferType.SPANNABLE)
    } else {
        view.text = text
    }
}

@BindingAdapter("optionalText")
fun setOptionalText(view: TextView, text: String?) {
    view.visibleOrGone(!text.isNullOrEmpty())
    view.text = text
}

@BindingAdapter("stateIcon")
fun setStateIcon(view: ImageView, recording: Recording?) {
    var drawable: Drawable? = null
    if (recording != null) {
        when {
            recording.isFailed -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_error_small)
            recording.isCompleted -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_success_small)
            recording.isMissed -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_error_small)
            recording.isRecording -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_rec_small)
            recording.isScheduled -> drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_schedule_small)
        }
    }

    view.visibleOrGone(drawable != null)
    view.setImageDrawable(drawable)
}

@BindingAdapter("iconUrl", "iconVisibility")
fun setChannelIcon(view: ImageView, iconUrl: String?, visible: Boolean) {
    if (visible) {
        setChannelIcon(view, iconUrl)
    } else {
        view.gone()
    }
}

/**
 * Loads the given program image via Glide into the image view
 *
 * @param view The view where the icon and visibility shall be applied to
 * @param url  The url of the channel icon
 */
@BindingAdapter("programImage", "programImageVisibility")
fun setProgramImage(view: ImageView, url: String?, visible: Boolean) {
    if (url.isNullOrEmpty() || !visible) {
        view.gone()
    } else {

        val transformation = object : Transformation {

            override fun transform(source: Bitmap): Bitmap {
                val targetWidth = view.width
                if (targetWidth == 0 || source.height == 0 || source.width == 0) {
                    Timber.d("Returning source image, target width is $targetWidth, source height is ${source.height}, source width is ${source.width}")
                    return source
                }
                val aspectRatio = source.height.toDouble() / source.width.toDouble()
                val targetHeight = (targetWidth * aspectRatio).toInt()
                val result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false)
                if (result != source) {
                    // Same bitmap is returned if sizes are the same
                    source.recycle()
                }
                Timber.d("Returning transformed image")
                return result
            }

            override fun key(): String {
                return "transformation" + " desiredWidth"
            }
        }

        Picasso.get()
                .load(url)
                .transform(transformation)
                .into(view, object : Callback {
                    override fun onSuccess() {
                        view.visible()
                    }

                    override fun onError(e: Exception) {
                        Timber.d("Could not load image $url")
                        view.gone()
                    }
                })
    }
}

/**
 * Loads the given channel icon via Glide into the image view
 *
 * @param view    The view where the icon and visibility shall be applied to
 * @param iconUrl The url of the channel icon
 */
@BindingAdapter("iconUrl")
fun setChannelIcon(view: ImageView, iconUrl: String?) {
    if (iconUrl.isNullOrEmpty()) {
        //Timber.d("Channel icon '$iconUrl' is empty or null, hiding icon")
        view.gone()
    } else {
        val url = getIconUrl(view.context, iconUrl)
        //Timber.d("Channel icon '$iconUrl' is not empty, loading icon from url '$url'")

        Picasso.get().cancelRequest(view)
        Picasso.get()
                .load(url)
                .into(view, object : Callback {
                    override fun onSuccess() {
                        //Timber.d("Successfully loaded channel icon from url '$url'")
                        view.visible()
                    }

                    override fun onError(e: Exception) {
                        //Timber.d("Error loading channel icon from url '$url'")
                        view.gone()
                    }
                })
    }
}

@BindingAdapter("iconName", "iconUrl", "iconVisibility")
fun setChannelName(view: TextView, name: String?, iconUrl: String?, visible: Boolean) {
    if (visible) {
        setChannelName(view, name, iconUrl)
    } else {
        view.gone()
    }
}

/**
 * Shows the channel name in the view if no channel icon exists.
 *
 * @param view    The view where the text and visibility shall be applied to
 * @param name    The name of the channel
 * @param iconUrl The url to the channel icon
 */
@BindingAdapter("iconName", "iconUrl")
fun setChannelName(view: TextView, name: String?, iconUrl: String?) {
    view.text = if (!name.isNullOrEmpty()) name else view.context.getString(R.string.all_channels)

    if (iconUrl.isNullOrEmpty()) {
        view.visible()
    } else {
        val url = getIconUrl(view.context, iconUrl)
        Picasso.get()
                .load(url).fetch(object : Callback {
                    override fun onSuccess() {
                        view.gone()
                    }

                    override fun onError(e: Exception) {
                        view.visible()
                    }
                })
    }
}

/**
 * Set the correct indication when the dual pane mode is active If the item is selected
 * the the arrow will be shown, otherwise only a vertical separation line is displayed.
 *
 * @param view       The view where the theme and background image shall be applied to
 * @param isSelected Determines if the background image shall show a selected state or not
 */
@BindingAdapter("backgroundImage")
fun setDualPaneBackground(view: ImageView, isSelected: Boolean) {
    if (isSelected) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.context)
        val lightTheme = sharedPreferences.getBoolean("light_theme_enabled", view.context.resources.getBoolean(R.bool.pref_default_light_theme_enabled))
        val icon = if (lightTheme) R.drawable.dual_pane_selector_active_light else R.drawable.dual_pane_selector_active_dark
        view.setBackgroundResource(icon)
    } else {
        val icon = R.drawable.dual_pane_selector_inactive
        view.setBackgroundResource(icon)
    }
}

/**
 * Converts the given number representing the days into a string with the
 * short names for the days. This string is assigned to the given view.
 *
 * @param view       The view where the short names for the days shall be shown
 * @param daysOfWeek The number representing the days of the week
 */
@BindingAdapter("daysText")
fun getDaysOfWeekText(view: TextView, daysOfWeek: Long) {
    val daysOfWeekList = view.context.resources.getStringArray(R.array.day_short_names)
    val text = StringBuilder()
    for (i in 0..6) {
        val s = if (daysOfWeek shr i and 1 == 1L) daysOfWeekList[i] else ""
        if (text.isNotEmpty() && s.isNotEmpty()) {
            text.append(", ")
        }
        text.append(s)
    }
    view.text = text.toString()
}

/**
 * Converts the given time in milliseconds into a default readable time
 * format, or if set by the preferences, into a localized time format
 *
 * @param view The view where the readable time shall be shown
 * @param time The time in milliseconds
 */
@BindingAdapter("timeText")
fun setLocalizedTime(view: TextView, time: Long) {
    if (time < 0) {
        view.text = view.context.getString(R.string.any)
        return
    }

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.context)
    val localizedTime = if (sharedPreferences.getBoolean("localized_date_time_format_enabled", view.context.resources.getBoolean(R.bool.pref_default_localized_date_time_format_enabled))) {
        // Show the date as defined with the currently active locale.
        // For the date display the short version will be used
        val df = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, getLocale(view.context))
        df.format(time)
    } else {
        // Show the date using the default format like 31.07.2013
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.format(time)
    }
    view.text = localizedTime
}

@BindingAdapter("dateText")
fun setLocalizedDate(view: TextView, date: Long) {
    if (date < 0) {
        view.text = view.context.getString(R.string.any)
        return
    }

    var localizedDate = ""
    val context = view.context

    if (DateUtils.isToday(date)) {
        // Show the string today
        localizedDate = context.getString(R.string.today)

    } else if (date < System.currentTimeMillis() + TWO_DAYS && date > System.currentTimeMillis() - TWO_DAYS) {
        // Show a string like "42 minutes ago"
        localizedDate = DateUtils.getRelativeTimeSpanString(
                date, System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS).toString()

    } else if (date < System.currentTimeMillis() + SIX_DAYS && date > System.currentTimeMillis() - TWO_DAYS) {
        // Show the day of the week, like Monday or Tuesday
        val sdf = SimpleDateFormat("EEEE", Locale.US)
        localizedDate = sdf.format(date)
    }

    // Translate the day strings, if the string is empty
    // use the day month year date representation
    when (localizedDate) {
        "today" -> localizedDate = context.getString(R.string.today)
        "tomorrow" -> localizedDate = context.getString(R.string.tomorrow)
        "in 2 days" -> localizedDate = context.getString(R.string.in_2_days)
        "Monday" -> localizedDate = context.getString(R.string.monday)
        "Tuesday" -> localizedDate = context.getString(R.string.tuesday)
        "Wednesday" -> localizedDate = context.getString(R.string.wednesday)
        "Thursday" -> localizedDate = context.getString(R.string.thursday)
        "Friday" -> localizedDate = context.getString(R.string.friday)
        "Saturday" -> localizedDate = context.getString(R.string.saturday)
        "Sunday" -> localizedDate = context.getString(R.string.sunday)
        "yesterday" -> localizedDate = context.getString(R.string.yesterday)
        "2 days ago" -> localizedDate = context.getString(R.string.two_days_ago)
        else -> {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            localizedDate = if (prefs.getBoolean("localized_date_time_format_enabled", false)) {
                // Show the date as defined with the currently active locale.
                // For the date display the short version will be used
                val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, getLocale(view.context))
                df.format(date)
            } else {
                // Show the date using the default format like 31.07.2013
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
                sdf.format(date)
            }
        }
    }
    view.text = localizedDate
}

/**
 * Calculates the genre color from the given content type and sets it as the
 * background color of the given view
 *
 * @param view            The view that displays the genre color as a background
 * @param contentType     The content type to calculate the color from
 * @param showGenreColors True to show the color, false otherwise
 * @param offset          Positive offset from 0 to 100 to increase the transparency of the color
 */
@BindingAdapter("genreColor", "showGenreColor", "genreColorAlphaOffset", "genreColorItemName")
fun setGenreColor(view: TextView, contentType: Int, showGenreColors: Boolean, offset: Int, itemName: String) {
    val context = view.context
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    if (showGenreColors) {
        var color = ContextCompat.getColor(view.context, android.R.color.transparent)
        if (contentType >= 0) {
            // Get the genre color from the content type
            color = R.color.EPG_OTHER
            var type = contentType / 16 - 1
            type = if (type < 0) 0 else type

            Timber.d("Received content type $contentType for $itemName, final color id is ${contentType / 16}")
            when (type) {
                0 -> color = ContextCompat.getColor(view.context, R.color.EPG_MOVIES)
                1 -> color = ContextCompat.getColor(view.context, R.color.EPG_NEWS)
                2 -> color = ContextCompat.getColor(view.context, R.color.EPG_SHOWS)
                3 -> color = ContextCompat.getColor(view.context, R.color.EPG_SPORTS)
                4 -> color = ContextCompat.getColor(view.context, R.color.EPG_CHILD)
                5 -> color = ContextCompat.getColor(view.context, R.color.EPG_MUSIC)
                6 -> color = ContextCompat.getColor(view.context, R.color.EPG_ARTS)
                7 -> color = ContextCompat.getColor(view.context, R.color.EPG_SOCIAL)
                8 -> color = ContextCompat.getColor(view.context, R.color.EPG_SCIENCE)
                9 -> color = ContextCompat.getColor(view.context, R.color.EPG_HOBBY)
                10 -> color = ContextCompat.getColor(view.context, R.color.EPG_SPECIAL)
            }

            // Get the color with the desired alpha value
            val transparencyValue = sharedPreferences.getInt("genre_color_transparency", Integer.valueOf(view.context.resources.getString(R.string.pref_default_genre_color_transparency)))
            var alpha = ((transparencyValue - offset).toFloat() / 100.0f * 255.0f).toInt()
            if (alpha < 0) {
                alpha = 0
            }
            color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        view.setBackgroundColor(color)
        view.visible()
    } else {
        view.gone()
    }
}

@BindingAdapter("activeIcon")
fun setConnectionActiveIcon(view: ImageView, isActive: Boolean) {
    // Set the active / inactive icon depending on the theme and selection status
    if (getThemeId(view.context) == R.style.CustomTheme_Light) {
        view.setImageResource(if (isActive) R.drawable.item_active_light else R.drawable.item_not_active_light)
    } else {
        view.setImageResource(if (isActive) R.drawable.item_active_dark else R.drawable.item_not_active_dark)
    }
}
