package com.example.translateapp.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Lightweight network check. Returns true if the device has an active
 * network with Internet capability.
 *
 * Does NOT require ACCESS_NETWORK_STATE permission — uses the public
 * ConnectivityManager API available since API 23.
 */
object ConnectivityHelper {

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
