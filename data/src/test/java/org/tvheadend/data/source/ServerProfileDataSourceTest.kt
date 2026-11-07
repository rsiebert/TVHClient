package org.tvheadend.data.source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.testutil.TestDatabase
import org.tvheadend.data.testutil.observedValue

/**
 * Robolectric test for [ServerProfileDataSource] against a real in-memory database.
 *
 * The list accessors are backed by blocking sync queries while the writes run on the injected
 * [Dispatchers.Unconfined] `ioScope` inline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerProfileDataSourceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase
    private lateinit var dataSource: ServerProfileDataSource

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        dataSource = ServerProfileDataSource(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun serverProfile(
            id: Int = 0,
            type: String? = ServerProfile.HTSP_PROFILE,
            name: String? = "Profile $id",
            uuid: String? = null
    ): ServerProfile {
        return ServerProfile(id = id, connectionId = 1, name = name, uuid = uuid, type = type)
    }

    @Test
    fun recordingProfilesAndNamesOnlyReturnRecordingProfiles() {
        dataSource.addItem(serverProfile(id = 1, type = ServerProfile.RECORDING_PROFILE, name = "Rec"))
        dataSource.addItem(serverProfile(id = 2, type = ServerProfile.HTSP_PROFILE, name = "Htsp"))

        assertThat(dataSource.recordingProfiles.map { it.id }).containsExactly(1)
        assertThat(dataSource.recordingProfileNames).asList().containsExactly("Rec")
    }

    @Test
    fun htspAndHttpPlaybackProfilesFilterByType() {
        dataSource.addItem(serverProfile(id = 1, type = ServerProfile.HTSP_PROFILE))
        dataSource.addItem(serverProfile(id = 2, type = ServerProfile.HTTP_PROFILE))

        assertThat(dataSource.htspPlaybackProfiles.map { it.id }).containsExactly(1)
        assertThat(dataSource.httpPlaybackProfiles.map { it.id }).containsExactly(2)
    }

    @Test
    fun addItemsStoresAllProfiles() {
        dataSource.addItems(listOf(serverProfile(id = 1), serverProfile(id = 2)))

        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(2)
    }

    @Test
    fun updateRemoveAndRemoveAll() {
        dataSource.addItem(serverProfile(id = 1, name = "Before"))
        dataSource.updateItem(serverProfile(id = 1, name = "After"))
        assertThat(db.serverProfileDao.loadProfileByIdSync(1)!!.name).isEqualTo("After")

        dataSource.removeItem(serverProfile(id = 1))
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(0)

        dataSource.addItems(listOf(serverProfile(id = 2), serverProfile(id = 3)))
        dataSource.removeAll()
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun getItemByIdResolvesByIntIdAndByUuid() {
        dataSource.addItem(serverProfile(id = 7, name = "ById", uuid = "uuid-7"))

        assertThat(dataSource.getItemById(7)!!.name).isEqualTo("ById")
        assertThat(dataSource.getItemById("uuid-7")!!.id).isEqualTo(7)
        assertThat(dataSource.getItemById("missing")).isNull()
        assertThat(dataSource.getItemById(999)).isNull()
        // Any other key type resolves to null.
        assertThat(dataSource.getItemById(7L)).isNull()
    }

    @Test
    fun notImplementedLiveDataGettersReturnEmpty() {
        dataSource.addItem(serverProfile(id = 1))

        assertThat(dataSource.getLiveDataItemCount().observedValue()).isNull()
        assertThat(dataSource.getLiveDataItems().observedValue()).isNull()
        assertThat(dataSource.getLiveDataItemById(1).observedValue()).isNull()
        assertThat(dataSource.getItems()).isEmpty()
    }
}
