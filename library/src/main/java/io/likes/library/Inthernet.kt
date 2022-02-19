package io.likes.library

import android.content.Context
import android.net.ConnectivityManager

internal object Inthernet {
    private val TAG = Inthernet::class.java.simpleName
    fun isInternetAvailable(context: Context): Boolean {
		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val info = cm.activeNetworkInfo
        return if (info == null) run { return false } else {
			if (info.isConnected) {
				true
			} else {
				true
			}
		}
    }
}
