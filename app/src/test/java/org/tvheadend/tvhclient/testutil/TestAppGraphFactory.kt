package org.tvheadend.tvhclient.testutil

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.tvheadend.data.AppRepository
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.source.ChannelDataSource
import org.tvheadend.data.source.ChannelTagDataSource
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.data.source.InputDataSource
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.data.source.ProgramDataSource
import org.tvheadend.data.source.RecordingDataSource
import org.tvheadend.data.source.SeriesRecordingDataSource
import org.tvheadend.data.source.ServerProfileDataSource
import org.tvheadend.data.source.ServerStatusDataSource
import org.tvheadend.data.source.SubscriptionDataSource
import org.tvheadend.data.source.TagAndChannelDataSource
import org.tvheadend.data.source.TimerRecordingDataSource
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.MainRepository
import org.tvheadend.tvhclient.util.billing.BillingDataSource
import timber.log.Timber

/**
 * Builds the small dependency graph the `:app` ViewModel tests need, mirroring the production
 * wiring in `data/src/main/java/org/tvheadend/data/di/DataContainer.kt` and
 * [org.tvheadend.tvhclient.MainApplication].
 *
 * The database is an in-memory [AppRoomDatabase] and every data source is given a
 * [Dispatchers.Unconfined] scope. That matters: the DAO write methods are non-suspending and
 * blocking, so `ioScope.launch { dao.insert(...) }` runs its body inline and returns at the very
 * next suspension point. Seeding data therefore takes effect immediately, with no polling, no
 * `Thread.sleep` and no test dispatcher juggling.
 */
object TestAppGraphFactory {

    /** A scope whose `launch` runs the (non-suspending) data source writes inline. */
    private fun inlineScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    /**
     * A fresh [AppRoomDatabase] in memory. `allowMainThreadQueries` is safe because the blocking
     * DAO calls run on the test thread and there is no real UI that could be stalled.
     */
    fun database(context: Context): AppRoomDatabase =
        Room.inMemoryDatabaseBuilder(context, AppRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    /** A real [AppRepository] over a new in-memory database. */
    fun appRepository(context: Context): AppRepository = appRepository(database(context))

    /** A real [AppRepository] over [db], wired exactly like `DataContainer`. */
    fun appRepository(db: AppRoomDatabase): AppRepository {
        val scope = inlineScope()
        return AppRepository(
            ChannelDataSource(db, scope),
            ProgramDataSource(db, scope),
            RecordingDataSource(db, scope),
            SeriesRecordingDataSource(db, scope),
            TimerRecordingDataSource(db, scope),
            ConnectionDataSource(db, scope),
            ChannelTagDataSource(db, scope),
            ServerStatusDataSource(db, scope),
            ServerProfileDataSource(db, scope),
            TagAndChannelDataSource(db, scope),
            MiscDataSource(db, scope),
            SubscriptionDataSource(db),
            InputDataSource(db)
        )
    }

    /**
     * A [BillingDataSource] mock with inert flows.
     *
     * `BillingDataSource.getInstance(...)` is deliberately not used: it builds a real Play Billing
     * client. [MainRepository] only needs the flows it exposes, so a relaxed-free MockK with those
     * four stubs is enough to construct it.
     *
     * @param newPurchases the shared flow backing `getNewPurchases()`; emit on it to drive
     *   [MainRepository]'s billing-message collection.
     */
    fun billingDataSource(
        isPurchased: Boolean = false,
        canPurchase: Boolean = true,
        billingFlowInProcess: Boolean = false,
        newPurchases: MutableSharedFlow<List<String>> = MutableSharedFlow()
    ): BillingDataSource {
        val billing = mockk<BillingDataSource>()
        every { billing.isPurchased(any()) } returns flowOf(isPurchased)
        every { billing.canPurchase(any()) } returns flowOf(canPurchase)
        every { billing.getNewPurchases() } returns newPurchases
        every { billing.getBillingFlowInProcess() } returns flowOf(billingFlowInProcess)
        return billing
    }

    /** A [MainRepository] over [billing], with a [Dispatchers.Unconfined] default scope. */
    fun mainRepository(billing: BillingDataSource = billingDataSource()): MainRepository =
        MainRepository(billing, inlineScope())

    /**
     * Builds a graph over [db] and installs it into [AppGraphProvider], so the next ViewModel that
     * resolves the graph picks it up. Remember to reset `AppGraphProvider.graph = null` in `@After`.
     */
    fun install(db: AppRoomDatabase): TestAppGraph {
        val graph = TestAppGraph(appRepository(db), mainRepository())
        AppGraphProvider.graph = graph
        return graph
    }

    /** Convenience overload that also creates (and returns) the in-memory database. */
    fun install(context: Context): TestAppGraph {
        val ctx = context.applicationContext
        val db = Room.inMemoryDatabaseBuilder(ctx, AppRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return install(db)
    }

    /**
     * Plants a no-op [Timber.Tree]. The ViewModels log through `Timber` and no tree is planted in a
     * plain JVM test, so the very first `Timber.d` would otherwise throw.
     */
    fun plantNoopTimber() {
        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
        })
    }
}

/**
 * Reads a [LiveData]'s current value by attaching a no-op observer.
 *
 * Room computes a query-backed LiveData only while it has observers. Combined with
 * [androidx.arch.core.executor.testing.InstantTaskExecutorRule] the computation runs synchronously
 * on the calling thread, so the value is available immediately without any waiting or polling.
 */
fun <T> LiveData<T>.observedValue(): T? {
    val observer = Observer<T> { }
    observeForever(observer)
    val value = this.value
    removeObserver(observer)
    return value
}
