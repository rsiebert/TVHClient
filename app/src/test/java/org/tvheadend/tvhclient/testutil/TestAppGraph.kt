package org.tvheadend.tvhclient.testutil

import org.tvheadend.data.AppRepository
import org.tvheadend.tvhclient.AppGraph
import org.tvheadend.tvhclient.MainRepository

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
    override val mainRepository: MainRepository
) : AppGraph
