package org.tvheadend.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerProfileEntity
import org.tvheadend.data.testutil.TestDatabase

/**
 * Robolectric test for [ServerProfileDao] against a real in-memory database.
 *
 * The `server_profiles` table is keyed by an auto-generated `id` and holds playback and recording
 * profiles per connection. Every query filters on the active connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServerProfileDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppRoomDatabase

    @Before
    fun setUp() {
        TestDatabase.plantNoopTimber()
        db = TestDatabase.create()
    }

    @After
    fun tearDown() {
        TestDatabase.teardown(db)
    }

    private fun insertActiveAndInactiveConnections() {
        db.connectionDao.insert(TestDatabase.activeConnection(id = 1))
        db.connectionDao.insert(TestDatabase.inactiveConnection(id = 2))
    }

    @Test
    fun itemCountSyncOnlyCountsTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 2))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 2, id = 3))

        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(2)
    }

    @Test
    fun loadProfilesByTypeOnlyReturnsTheActiveConnectionProfiles() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1, type = ServerProfile.HTSP_PROFILE))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 2, type = ServerProfile.HTTP_PROFILE))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 3, type = ServerProfile.RECORDING_PROFILE))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 2, id = 4, type = ServerProfile.HTSP_PROFILE))

        assertThat(db.serverProfileDao.loadHtspPlaybackProfilesSync().map { it.id }).containsExactly(1)
        assertThat(db.serverProfileDao.loadHttpPlaybackProfilesSync().map { it.id }).containsExactly(2)
        assertThat(db.serverProfileDao.loadAllRecordingProfilesSync().map { it.id }).containsExactly(3)
    }

    @Test
    fun loadProfileByIdOnlyReturnsProfilesOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1, name = "Active"))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 2, id = 5, name = "Inactive"))

        assertThat(db.serverProfileDao.loadProfileByIdSync(1)!!.name).isEqualTo("Active")
        assertThat(db.serverProfileDao.loadProfileByIdSync(5)).isNull()
    }

    @Test
    fun loadProfileByUuidOnlyReturnsProfilesOfTheActiveConnection() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1, uuid = "uuid-active"))
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 2, id = 5, uuid = "uuid-inactive"))

        assertThat(db.serverProfileDao.loadProfileByUuidSync("uuid-active")!!.id).isEqualTo(1)
        assertThat(db.serverProfileDao.loadProfileByUuidSync("uuid-inactive")).isNull()
        assertThat(db.serverProfileDao.loadProfileByUuidSync("missing")).isNull()
    }

    @Test
    fun insertSingleAndListWithIgnoreConflictStrategy() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 10, name = "Ten"))
        db.serverProfileDao.insert(listOf(
                TestDatabase.serverProfile(connectionId = 1, id = 11, name = "Eleven"),
                TestDatabase.serverProfile(connectionId = 1, id = 12, name = "Twelve")))
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(3)

        // Inserting the same id again is ignored, it does not overwrite or throw.
        db.serverProfileDao.insert(listOf(TestDatabase.serverProfile(connectionId = 1, id = 11, name = "Ignored")))

        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(3)
        assertThat(db.serverProfileDao.loadProfileByIdSync(11)!!.name).isEqualTo("Eleven")
    }

    @Test
    fun updateDeleteAndDeleteAll() {
        insertActiveAndInactiveConnections()
        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 1, name = "Before"))

        db.serverProfileDao.update(TestDatabase.serverProfile(connectionId = 1, id = 1, name = "After"))
        assertThat(db.serverProfileDao.loadProfileByIdSync(1)!!.name).isEqualTo("After")

        db.serverProfileDao.delete(TestDatabase.serverProfile(connectionId = 1, id = 1))
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(0)

        db.serverProfileDao.insert(TestDatabase.serverProfile(connectionId = 1, id = 2))
        db.serverProfileDao.deleteAll()
        assertThat(db.serverProfileDao.itemCountSync).isEqualTo(0)
    }

    @Test
    fun mapsServerProfileToEntityAndBack() {
        val serverProfile = ServerProfile(
                id = 3,
                connectionId = 1,
                isEnabled = true,
                name = "Profile",
                uuid = "uuid",
                comment = "Comment",
                type = ServerProfile.HTTP_PROFILE)

        val restored = ServerProfileEntity.from(serverProfile).toServerProfile()

        assertThat(restored).isEqualTo(serverProfile)
    }
}
