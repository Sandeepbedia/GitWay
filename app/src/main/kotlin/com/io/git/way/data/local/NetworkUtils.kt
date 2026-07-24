package com.io.git.way.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Pre-upload connectivity check (422-fix PRD §2 "Internet connection available"). */
object NetworkUtils {

    fun isOnline(context: Context): Boolean {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true // fail open: don't block the upload if we can't even ask
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
