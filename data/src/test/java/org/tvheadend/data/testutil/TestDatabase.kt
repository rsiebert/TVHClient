package org.tvheadend.data.testutil

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ChannelEntity
import org.tvheadend.data.entity.ChannelTagEntity
import org.tvheadend.data.entity.ConnectionEntity
import org.tvheadend.data.entity.ProgramEntity
import org.tvheadend.data.entity.RecordingEntity
import org.tvheadend.data.entity.SeriesRecordingEntity
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerProfileEntity
import org.tvheadend.data.entity.ServerStatusEntity
import org.tvheadend.data.entity.TagAndChannelEntity
import org.tvheadend.data.entity.TimerRecordingEntity
import timber.log.Timber

/**
 * Helpers for the JVM (Robolectric) tests of the `:data` module.
 *
 * The data sources normally talk to the process-wide [AppRoomDatabase.getInstance] singleton,
 * which is backed by a file. Tests must never touch it: it would leak state between tests and
 * between test classes. Everything here builds a throw-away in-memory database instead.
 */
internal object TestDatabase {

    /**
     * Builds a fresh [AppRoomDatabase] in memory.
     *
     * `allowMainThreadQueries` is safe here because the blocking DAO calls are executed on the
     * test thread and there is no real UI that could be stalled.
     */
    fun create(): AppRoomDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppRoomDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    /** Closes the in-memory database created by [create]. */
    fun teardown(db: AppRoomDatabase) {
        db.close()
    }

    /**
     * Plants a no-op [Timber.Tree].
     *
     * The data sources log via `Timber.d`, but no tree is planted in a plain JVM test, so the
     * very first log call would otherwise throw.
     */
    fun plantNoopTimber() {
        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
        })
    }

    /**
     * An active connection with the given [id].
     *
     * [ConnectionEntity.id] is `autoGenerate`, but inserting a non-zero id uses it verbatim, so
     * this lets tests control the id.
     */
    fun activeConnection(id: Int = 1): ConnectionEntity {
        return ConnectionEntity(id = id, name = "Active", isActive = true)
    }

    /** An inactive connection with the given [id]. */
    fun inactiveConnection(id: Int = 2): ConnectionEntity {
        return ConnectionEntity(id = id, name = "Inactive", isActive = false)
    }

    /** A [ChannelEntity] belonging to [connectionId], with values derived from [id] by default. */
    fun channel(
            id: Int,
            connectionId: Int,
            name: String? = "Channel $id",
            number: Int = id,
            displayNumber: String? = id.toString(),
            serverOrder: Int = id
    ): ChannelEntity {
        return ChannelEntity(
                id = id,
                number = number,
                name = name,
                connectionId = connectionId,
                displayNumber = displayNumber,
                serverOrder = serverOrder)
    }

    /**
     * A [ProgramEntity] with the given primary key ([eventId], [connectionId]).
     *
     * The start/stop window and title default to values derived from [eventId] so individual
     * tests only have to override what they actually assert on.
     */
    fun program(
            eventId: Int,
            connectionId: Int,
            channelId: Int = 0,
            start: Long = 0L,
            stop: Long = 1_000L,
            title: String? = "Program $eventId",
            subtitle: String? = null,
            contentType: Int = 0
    ): ProgramEntity {
        return ProgramEntity(
                eventId = eventId,
                channelId = channelId,
                start = start,
                stop = stop,
                title = title,
                subtitle = subtitle,
                contentType = contentType,
                connectionId = connectionId)
    }

    /**
     * A [RecordingEntity] with the given primary key ([id], [connectionId]) that is `completed`
     * unless [state]/[error] say otherwise.
     */
    fun recording(
            id: Int,
            connectionId: Int,
            channelId: Int = 0,
            start: Long = 0L,
            stop: Long = 1_000L,
            title: String? = "Recording $id",
            state: String? = "completed",
            error: String? = null,
            contentType: Int = 0,
            duration: Int = 0,
            duplicate: Int = 0,
            eventId: Int = 0
    ): RecordingEntity {
        return RecordingEntity(
                id = id,
                channelId = channelId,
                start = start,
                stop = stop,
                title = title,
                state = state,
                error = error,
                contentType = contentType,
                duration = duration,
                duplicate = duplicate,
                eventId = eventId,
                connectionId = connectionId)
    }

    /** A [ChannelTagEntity] with the given primary key ([tagId], [connectionId]). */
    fun channelTag(
            tagId: Int,
            connectionId: Int,
            tagName: String? = "Tag $tagId",
            channelCount: Int = 0,
            isSelected: Boolean = false
    ): ChannelTagEntity {
        return ChannelTagEntity(
                tagId = tagId,
                tagName = tagName,
                channelCount = channelCount,
                connectionId = connectionId,
                isSelected = isSelected)
    }

    /** A [TagAndChannelEntity] linking [tagId] to [channelId] on [connectionId]. */
    fun tagAndChannel(tagId: Int, channelId: Int, connectionId: Int): TagAndChannelEntity {
        return TagAndChannelEntity(tagId = tagId, channelId = channelId, connectionId = connectionId)
    }

    /**
     * A [ServerStatusEntity] for [connectionId].
     *
     * [ServerStatusEntity.id] is `autoGenerate`, but a non-zero [id] is used verbatim, which lets
     * tests control the primary key.
     */
    fun serverStatus(
            connectionId: Int,
            id: Int = 0,
            connectionName: String? = null,
            serverName: String? = null,
            serverVersion: String? = null
    ): ServerStatusEntity {
        return ServerStatusEntity(
                id = id,
                connectionId = connectionId,
                connectionName = connectionName,
                serverName = serverName,
                serverVersion = serverVersion)
    }

    /**
     * A [ServerProfileEntity] for [connectionId] of [type] ([ServerProfile.HTSP_PROFILE] by
     * default). [ServerProfileEntity.id] is `autoGenerate`, but a non-zero [id] is used verbatim.
     */
    fun serverProfile(
            connectionId: Int,
            id: Int = 0,
            type: String? = ServerProfile.HTSP_PROFILE,
            name: String? = "Profile $id",
            uuid: String? = null,
            isEnabled: Boolean = false
    ): ServerProfileEntity {
        return ServerProfileEntity(
                id = id,
                connectionId = connectionId,
                isEnabled = isEnabled,
                name = name,
                uuid = uuid,
                type = type)
    }

    /** A [TimerRecordingEntity] with the given primary key ([id], [connectionId]). */
    fun timerRecording(
            id: String,
            connectionId: Int,
            channelId: Int = 0,
            title: String? = id,
            start: Long = 0L,
            stop: Long = 0L
    ): TimerRecordingEntity {
        return TimerRecordingEntity(
                id = id,
                title = title,
                channelId = channelId,
                start = start,
                stop = stop,
                connectionId = connectionId)
    }

    /** A [SeriesRecordingEntity] with the given primary key ([id], [connectionId]). */
    fun seriesRecording(
            id: String,
            connectionId: Int,
            channelId: Int = 0,
            title: String? = id,
            start: Long = 0L,
            startWindow: Long = 0L
    ): SeriesRecordingEntity {
        return SeriesRecordingEntity(
                id = id,
                title = title,
                channelId = channelId,
                start = start,
                startWindow = startWindow,
                connectionId = connectionId)
    }
}

/**
 * Reads a [LiveData]'s current value by attaching a no-op observer.
 *
 * Room computes a query-backed LiveData only while it has observers. Combined with
 * [androidx.arch.core.executor.testing.InstantTaskExecutorRule] the computation runs
 * synchronously on the calling thread, so the value is available immediately without any
 * waiting, polling or sleeping.
 */
fun <T> LiveData<T>.observedValue(): T? {
    val observer = Observer<T> { }
    observeForever(observer)
    val value = this.value
    removeObserver(observer)
    return value
}
