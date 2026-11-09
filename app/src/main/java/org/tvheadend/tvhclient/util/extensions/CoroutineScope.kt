package org.tvheadend.tvhclient.util.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit
) = launch {
    onPreExecute() // runs in Main Thread
    // runCatching keeps a failure in doInBackground from crashing the caller's scope (for example
    // a lifecycleScope) and simply skips onPostExecute in that case.
    val result = withContext(Dispatchers.IO) {
        runCatching { doInBackground() } // runs in background thread without blocking the Main Thread
    }
    result.onSuccess(onPostExecute) // runs in Main Thread
            .onFailure { Timber.e(it, "Background task failed") }
}
