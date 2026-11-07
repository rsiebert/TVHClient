package org.tvheadend.tvhclient

import android.app.Activity
import android.app.Application
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.testutil.TestAppGraphFactory
import org.tvheadend.tvhclient.util.billing.BillingDataSource

/**
 * Tests for [MainRepository].
 *
 * It wraps a `BillingDataSource`, which needs a live Play Billing client, so the data source is a
 * MockK fake. A [Dispatchers.Unconfined] default scope is injected so the billing-message
 * collection launched in `init` runs inline, which keeps the `messages` assertions deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainRepositoryTest {

    @Before
    fun setUp() {
        TestAppGraphFactory.plantNoopTimber()
    }

    private fun repository(billing: BillingDataSource) =
        MainRepository(billing, CoroutineScope(Dispatchers.Unconfined))

    // ---- billing flows --------------------------------------------------------------

    @Test
    fun isPurchasedReturnsTheBillingFlow() = runBlocking {
        val repository = repository(TestAppGraphFactory.billingDataSource(isPurchased = true))

        assertThat(repository.isPurchased(MainRepository.UNLOCKER).first()).isTrue()
    }

    @Test
    fun canPurchaseReturnsTheBillingFlow() = runBlocking {
        val repository = repository(TestAppGraphFactory.billingDataSource(canPurchase = false))

        assertThat(repository.canPurchase(MainRepository.UNLOCKER).first()).isFalse()
    }

    @Test
    fun billingFlowInProcessReturnsTheBillingFlow() = runBlocking {
        val repository = repository(TestAppGraphFactory.billingDataSource(billingFlowInProcess = true))

        assertThat(repository.billingFlowInProcess.first()).isTrue()
    }

    // ---- messages -------------------------------------------------------------------

    @Test
    fun sendMessageEmitsOnTheMessagesFlow() = runBlocking {
        val repository = repository(TestAppGraphFactory.billingDataSource())

        repository.messages.test {
            repository.sendMessage(R.string.pref_unlocker)
            assertThat(awaitItem()).isEqualTo(R.string.pref_unlocker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aNewUnlockerPurchaseIsForwardedAsAMessage() = runBlocking {
        val purchases = MutableSharedFlow<List<String>>()
        val repository = repository(TestAppGraphFactory.billingDataSource(newPurchases = purchases))

        repository.messages.test {
            purchases.emit(listOf(MainRepository.UNLOCKER))
            assertThat(awaitItem()).isEqualTo(R.string.pref_unlocker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- delegation -----------------------------------------------------------------

    @Test
    fun buySkuDelegatesToTheBillingDataSource() {
        val billing = mockk<BillingDataSource>(relaxed = true)
        val repository = repository(billing)
        val activity = mockk<Activity>()

        repository.buySku(activity, MainRepository.UNLOCKER)

        verify { billing.launchBillingFlow(activity, MainRepository.UNLOCKER) }
    }

    @Test
    fun refreshPurchasesDelegatesToTheBillingDataSource() = runBlocking {
        val billing = mockk<BillingDataSource>(relaxed = true)
        val repository = repository(billing)

        repository.refreshPurchases()

        coVerify { billing.refreshPurchases() }
    }
}
