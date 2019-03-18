@file:JvmName("NetworkUtils")

package org.tvheadend.tvhclient.ui.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import timber.log.Timber
import java.lang.reflect.InvocationTargetException

fun isConnectionAvailable(context: Context): Boolean {
    return isNetworkAvailable(context) || isWifiApEnabled(context)
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

private fun isWifiApEnabled(context: Context): Boolean {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wmMethods = wifiManager.javaClass.declaredMethods
    for (method in wmMethods) {
        if (method.name == "isWifiApEnabled") {
            try {
                return method.invoke(wifiManager) as Boolean
            } catch (e: IllegalArgumentException) {
                Timber.d("Illegal argument error while invoking method 'isWifiApEnabled'", e)
            } catch (e: IllegalAccessException) {
                Timber.d("Illegal access error while invoking method 'isWifiApEnabled'", e)
            } catch (e: InvocationTargetException) {
                Timber.d("Invocation error while invoking method 'isWifiApEnabled'", e)
            }
        }
    }
    return false
}

