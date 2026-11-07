package org.tvheadend.tvhclient

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tvheadend.htsp.FakeHtspServer
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsListConnectionsFragment

/**
 * Instrumented (Espresso) test for the "add connection" flow.
 *
 * ### What the "add connection" UI actually does
 * Contrary to what you might expect, saving a new connection performs **no** network I/O:
 *
 *  - [org.tvheadend.tvhclient.ui.features.settings.SettingsAddConnectionFragment.save]
 *    only calls `ConnectionValidator.isConnectionInputValid(connection)`, which is a purely
 *    syntactic check (non-empty/valid name, `http(s)://host:port` url, valid WOL settings).
 *  - It then persists the connection (`SettingsViewModel.addConnection`) and pops itself off
 *    the back stack, returning to the connection list.
 *
 * The HTSP (and HTTP) connection is only attempted later, by `ConnectionService`, for the
 * *active* connection. So the fake HTSP server is **not required** for this particular screen
 * to validate and navigate onward.
 *
 * The test nevertheless starts the fake server in-process and points the connection's urls at
 * it, because:
 *  - it keeps the app and the (would-be) server co-located exactly like a real setup, and
 *  - it documents the fact above: if a future change makes the add screen talk to the server,
 *    the fake server is already there to answer the handshake.
 *
 * The "next screen" the app reaches after saving is the connection list, which is what we
 * assert here.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsAddConnectionEspressoTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<SettingsActivity>(
        Intent(ApplicationProvider.getApplicationContext<Context>(), SettingsActivity::class.java)
            .putExtra("setting_type", "list_connections"),
    )

    @Test
    fun savingAConnectionNavigatesBackToTheConnectionList() {
        // A per-run unique name so the assertion is never ambiguous with connections left
        // behind by previous runs. It only contains characters the name validator accepts.
        val connectionName = "TestServer" + System.nanoTime()

        // 1. The settings activity starts on the connection list screen.
        onView(withText(R.string.pref_connections)).check(matches(isDisplayed()))

        // 2. Open the add-connection screen via the toolbar "Add" action.
        onView(withText(R.string.add)).perform(click())
        onView(withText(R.string.add_connection)).check(matches(isDisplayed()))

        // 3. Fill in the connection details. The urls point at the fake HTSP server that runs
        //    inside this instrumentation process.
        setPreference(R.string.pref_name, connectionName)
        setPreference(R.string.pref_server_url, server.url)
        setPreference(R.string.pref_streaming_url, server.url)
        setPreference(R.string.pref_user, USERNAME)
        setPreference(R.string.pref_pass, PASSWORD)

        // 4. Save. This validates + persists the connection and pops the add screen.
        onView(withText(R.string.save)).perform(click())

        // 5. The app has navigated back to the connection list screen.
        activityRule.scenario.onActivity { activity ->
            val current = activity.supportFragmentManager.findFragmentById(R.id.main)
            assertTrue(
                "Expected to be back on the connection list screen after saving, but found $current",
                current is SettingsListConnectionsFragment,
            )
        }
        onView(withText(R.string.pref_connections)).check(matches(isDisplayed()))

        // 6. The newly added connection is listed (the list is populated asynchronously
        //    from the database, so poll for it).
        waitUntilDisplayed(connectionName)
    }

    /** Opens an [androidx.preference.EditTextPreference] dialog and enters [value]. */
    private fun setPreference(titleRes: Int, value: String) {
        scrollToPreference(titleRes)
        onView(withText(titleRes)).perform(click())
        onView(withId(android.R.id.edit)).perform(replaceText(value), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click())
    }

    /**
     * The preference screen is a RecyclerView, so the entry has to be scrolled into view
     * before it can be interacted with.
     */
    private fun scrollToPreference(titleRes: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (isDisplayed(titleRes)) {
                return
            }
            try {
                onView(isRoot()).perform(swipeUp())
            } catch (ignore: Throwable) {
                // Nothing to scroll.
            }
            Thread.sleep(100)
        }
    }

    private fun isDisplayed(titleRes: Int): Boolean {
        return try {
            onView(withText(titleRes)).check(matches(isDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun waitUntilDisplayed(text: String, timeoutMs: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withText(text)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                lastError = e
                // The list is filled asynchronously and the new entry may be below the fold.
                try {
                    onView(withId(R.id.recycler_view)).perform(swipeUp())
                } catch (ignore: Throwable) {
                    // Nothing to scroll (yet).
                }
                Thread.sleep(200)
            }
        }
        throw AssertionError("'$text' was not displayed within ${timeoutMs}ms", lastError)
    }

    companion object {
        private const val USERNAME = "*"
        private const val PASSWORD = "test"

        private lateinit var server: FakeHtspServer

        @JvmStatic
        @BeforeClass
        fun startFakeServer() {
            server = FakeHtspServer(username = USERNAME, password = PASSWORD).start()
        }

        @JvmStatic
        @AfterClass
        fun stopFakeServer() {
            server.close()
        }
    }
}
