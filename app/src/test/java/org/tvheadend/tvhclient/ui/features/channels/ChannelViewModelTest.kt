package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.testutil.TestAppGraph
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.testutil.observedValue

/**
 * Tests for [ChannelViewModel] and its base class [BaseChannelViewModel].
 *
 * All channel/tag queries are scoped to the *active* connection, so every test seeds one first.
 * Data is written through the public `*DataSource` APIs with the public entity types, which (via
 * the `Unconfined` scope in [TestAppGraphFactory]) is applied synchronously.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChannelViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Application
    private lateinit var db: AppRoomDatabase
    private lateinit var graph: TestAppGraph

    @Before
    fun setUp() {
        TestAppGraphFactory.plantNoopTimber()
        context = ApplicationProvider.getApplicationContext()
        db = TestAppGraphFactory.database(context)
        graph = TestAppGraphFactory.install(db)
    }

    @After
    fun tearDown() {
        AppGraphProvider.graph = null
        db.close()
    }

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(context)

    /** Adds an active connection and returns its id (the id the channel/tag rows must reference). */
    private fun seedActiveConnection(): Int = runBlocking {
        graph.appRepository.connectionData.addItem(Connection(name = "Active", isActive = true))
        graph.appRepository.connectionData.getActiveItem().id
    }

    // ---- channelCount ---------------------------------------------------------------

    @Test
    fun channelCountReflectsTheStoredChannelsOfTheActiveConnection() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, name = "One"))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, name = "Two"))

        val vm = ChannelViewModel(context)

        assertThat(vm.channelCount.observedValue()).isEqualTo(2)
    }

    // ---- showAllChannelTags ---------------------------------------------------------

    @Test
    fun showAllChannelTagsDefaultsToTrue() {
        val vm = ChannelViewModel(context)

        assertThat(vm.showAllChannelTags.value).isTrue()
    }

    @Test
    fun showAllChannelTagsFollowsThePreference() {
        prefs().edit().putBoolean("empty_channel_tags_enabled", false).commit()

        val vm = ChannelViewModel(context)

        assertThat(vm.showAllChannelTags.value).isFalse()
    }

    // ---- getSelectedChannelTagName --------------------------------------------------

    @Test
    fun getSelectedChannelTagNameReturnsUnknownBeforeTheTagsAreLoaded() {
        val vm = ChannelViewModel(context)

        assertThat(vm.getSelectedChannelTagName(context)).isEqualTo(context.getString(R.string.unknown))
    }

    @Test
    fun getSelectedChannelTagNameReturnsTheSingleSelectedTag() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 5, tagName = "Movies", channelCount = 1, connectionId = connectionId, isSelected = true))
        val vm = ChannelViewModel(context)
        vm.channelTags.observeForever { }
        vm.selectedChannelTagIds.observeForever { }

        assertThat(vm.getSelectedChannelTagName(context)).isEqualTo("Movies")
    }

    @Test
    fun getSelectedChannelTagNameReturnsAllChannelsWhenNothingIsSelected() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 5, tagName = "Movies", channelCount = 1, connectionId = connectionId))
        val vm = ChannelViewModel(context)
        vm.channelTags.observeForever { }
        vm.selectedChannelTagIds.observeForever { }

        assertThat(vm.getSelectedChannelTagName(context)).isEqualTo(context.getString(R.string.all_channels))
    }

    @Test
    fun getSelectedChannelTagNameReturnsMultipleWhenMoreThanOneTagIsSelected() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 5, tagName = "Movies", channelCount = 1, connectionId = connectionId, isSelected = true))
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 6, tagName = "News", channelCount = 1, connectionId = connectionId, isSelected = true))
        val vm = ChannelViewModel(context)
        vm.channelTags.observeForever { }
        vm.selectedChannelTagIds.observeForever { }

        assertThat(vm.getSelectedChannelTagName(context)).isEqualTo(context.getString(R.string.multiple_channel_tags))
    }

    // ---- setSelectedChannelTagIds ---------------------------------------------------

    @Test
    fun setSelectedChannelTagIdsPersistsTheSelection(): Unit = runBlocking {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 5, tagName = "Movies", channelCount = 1, connectionId = connectionId))
        graph.appRepository.channelTagData.addItem(
            ChannelTag(tagId = 6, tagName = "News", channelCount = 1, connectionId = connectionId))
        val vm = ChannelViewModel(context)

        vm.setSelectedChannelTagIds(setOf(6))

        assertThat(vm.selectedChannelTagIds.observedValue()).containsExactly(6)
        assertThat(graph.appRepository.channelTagData.getItemById(5)!!.isSelected).isFalse()
        assertThat(graph.appRepository.channelTagData.getItemById(6)!!.isSelected).isTrue()
    }

    // ---- sort order -----------------------------------------------------------------

    @Test
    fun channelsFollowTheConfiguredSortOrderPreference() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, name = "One", serverOrder = 2))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, name = "Two", serverOrder = 1))
        // 2 == "sort by channel id ascending".
        prefs().edit().putString("channel_sort_order", "2").commit()

        val vm = ChannelViewModel(context)

        assertThat(vm.channels.observedValue()!!.map { it.id }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun changingTheSortOrderPreferenceReloadsTheChannels() {
        val connectionId = seedActiveConnection()
        graph.appRepository.channelData.addItem(Channel(id = 1, connectionId = connectionId, name = "One", serverOrder = 2))
        graph.appRepository.channelData.addItem(Channel(id = 2, connectionId = connectionId, name = "Two", serverOrder = 1))
        prefs().edit().putString("channel_sort_order", "2").commit()
        val vm = ChannelViewModel(context)
        vm.channels.observeForever { }
        assertThat(vm.channels.value!!.map { it.id }).isEqualTo(listOf(1, 2))

        // 0 == "sort by server order ascending".
        prefs().edit().putString("channel_sort_order", "0").commit()
        vm.onSharedPreferenceChanged(prefs(), "channel_sort_order")

        assertThat(vm.channels.value!!.map { it.id }).isEqualTo(listOf(2, 1))
    }

    // ---- selectedTime ---------------------------------------------------------------

    @Test
    fun setSelectedTimeUpdatesTheSelectedTimeOnlyWhenItChanges() {
        val vm = ChannelViewModel(context)
        val before = vm.selectedTime.value

        vm.setSelectedTime(before!!)
        assertThat(vm.selectedTime.value).isEqualTo(before)

        vm.setSelectedTime(before + 1000)
        assertThat(vm.selectedTime.value).isEqualTo(before + 1000)
    }
}
