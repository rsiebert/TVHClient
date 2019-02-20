package org.tvheadend.tvhclient.util.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class NetworkUtils {

    public static boolean isConnectionAvailable(@NonNull Context context) {
        return isNetworkAvailable(context) || NetworkUtils.isWifiApEnabled(context);
    }

    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        }
        return false;
    }

    private static boolean isWifiApEnabled(@NonNull Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    return (boolean) method.invoke(wifiManager);
                } catch (IllegalArgumentException e) {
                    Timber.d("Illegal argument error while invoking method 'isWifiApEnabled'", e);
                } catch (IllegalAccessException e) {
                    Timber.d("Illegal access error while invoking method 'isWifiApEnabled'", e);
                } catch (InvocationTargetException e) {
                    Timber.d("Invokation error while invoking method 'isWifiApEnabled'", e);
                }
            }
        }
        return false;
    }
}
