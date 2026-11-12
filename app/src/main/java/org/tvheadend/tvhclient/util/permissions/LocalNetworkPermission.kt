package org.tvheadend.tvhclient.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android 17 (API level 37) gates all local network traffic behind the `ACCESS_LOCAL_NETWORK`
 * runtime permission. TVHClient is a pure LAN client - the HTSP connection, the Wake-on-LAN
 * packet and the Cast discovery all talk to a server on the local network - so without this
 * permission those operations are blocked by the platform: a TCP connection silently times out
 * and a UDP broadcast fails with `EPERM`.
 *
 * On devices below Android 17 the permission does not exist and local network access is granted
 * implicitly through `INTERNET`, so nothing has to be requested there.
 */
object LocalNetworkPermission {

    /** Request code used by [request]; the caller handles it in `onRequestPermissionsResult`. */
    const val REQUEST_CODE = 1001

    /** API level that introduced the permission and turned the gating on. */
    private const val MIN_API_LEVEL = 37

    /** True when the platform requires the runtime permission (Android 17 and up). */
    fun isRequired(): Boolean = Build.VERSION.SDK_INT >= MIN_API_LEVEL

    /** True when local network access may be used, either because it is not gated or it is granted. */
    fun isGranted(context: Context): Boolean =
            !isRequired() ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                    PackageManager.PERMISSION_GRANTED

    /** True when the permission still has to be requested before local network access works. */
    fun needsRequest(context: Context): Boolean = !isGranted(context)

    /**
     * Asks the user for the permission. The result is delivered to
     * [Activity.onRequestPermissionsResult] under [REQUEST_CODE]; a no-op below Android 17.
     */
    fun request(activity: Activity) {
        if (!isRequired()) {
            return
        }
        ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK),
                REQUEST_CODE,
        )
    }
}
