package org.tvheadend.tvhclient.testutil

import org.tvheadend.data.AppRepository
import org.tvheadend.tvhclient.AppGraph
import org.tvheadend.tvhclient.MainRepository
import org.tvheadend.tvhclient.util.HtspExecutor
import java.util.concurrent.ScheduledExecutorService

/**
 * A lightweight [AppGraph] for the `:app` JVM tests.
 *
 * The production graph is [org.tvheadend.tvhclient.MainApplication], whose `onCreate` performs
 * Firebase, Cast, Billing and database-migration work that has nothing to do with the code under
 * test. This implementation instead holds:
 *
 * * a real [AppRepository] built over a throw-away, in-memory Room database (so the data sources
 *   and their queries behave exactly as in production), and
 * * a [MainRepository] whose `BillingDataSource` is a MockK fake (the real one needs a live Play
 *   Billing client).
 *
 * Install it via [TestAppGraphFactory.install] before constructing a ViewModel.
 */
class TestAppGraph(
    override val appRepository: AppRepository,
    override val mainRepository: MainRepository,
    /**
     * A small pool of its own so a test can supply or replace it. The threads are daemons and time
     * out when idle, so a test that does not shut it down does not keep the JVM alive.
     */
    override val htspExecutor: ScheduledExecutorService = HtspExecutor.create(corePoolSize = 1)
) : AppGraph
