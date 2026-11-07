package org.tvheadend.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.source.*
import org.tvheadend.data.testutil.TestDatabase

/**
 * Robolectric test that builds a real [AppRepository] from real `*DataSource` instances over an
 * in-memory database, mirroring how [org.tvheadend.data.di.DataContainer] constructs it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        repository = AppRepository(
                ChannelDataSource(db),
                ProgramDataSource(db),
                RecordingDataSource(db),
                SeriesRecordingDataSource(db),
                TimerRecordingDataSource(db),
                ConnectionDataSource(db),
                ChannelTagDataSource(db),
                ServerStatusDataSource(db),
                ServerProfileDataSource(db),
                TagAndChannelDataSource(db),
                MiscDataSource(db),
                SubscriptionDataSource(db),
                InputDataSource(db))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    @Test
    fun constructorWiresUpEveryDataSource() {
        assertThat(repository.channelData).isInstanceOf(ChannelDataSource::class.java)
        assertThat(repository.programData).isInstanceOf(ProgramDataSource::class.java)
        assertThat(repository.recordingData).isInstanceOf(RecordingDataSource::class.java)
        assertThat(repository.seriesRecordingData).isInstanceOf(SeriesRecordingDataSource::class.java)
        assertThat(repository.timerRecordingData).isInstanceOf(TimerRecordingDataSource::class.java)
        assertThat(repository.connectionData).isInstanceOf(ConnectionDataSource::class.java)
        assertThat(repository.channelTagData).isInstanceOf(ChannelTagDataSource::class.java)
        assertThat(repository.serverStatusData).isInstanceOf(ServerStatusDataSource::class.java)
        assertThat(repository.serverProfileData).isInstanceOf(ServerProfileDataSource::class.java)
        assertThat(repository.tagAndChannelData).isInstanceOf(TagAndChannelDataSource::class.java)
        assertThat(repository.miscData).isInstanceOf(MiscDataSource::class.java)
        assertThat(repository.subscriptionData).isInstanceOf(SubscriptionDataSource::class.java)
        assertThat(repository.inputData).isInstanceOf(InputDataSource::class.java)
    }

    @Test
    fun isUnlockedLiveDataDefaultsToFalse() {
        assertThat(isUnlockedLiveData().value).isFalse()
    }

    /**
     * [AppRepository.isUnlockedLiveData] is a `private` backing field with no public accessor, and
     * the task forbids changing production visibility. Reading it reflectively lets the test still
     * assert its initial value without modifying production code.
     */
    private fun isUnlockedLiveData(): LiveData<Boolean> {
        val field = AppRepository::class.java.getDeclaredField("isUnlockedLiveData")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(repository) as LiveData<Boolean>
    }
}
