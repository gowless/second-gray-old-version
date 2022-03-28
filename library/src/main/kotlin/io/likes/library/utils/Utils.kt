package io.likes.library.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object Utils {




     suspend fun getAdvId(activity: AppCompatActivity): String = suspendCoroutine {
        val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity)
        val gadid = adInfo.id.toString()
        it.resume(gadid)
    }

        fun concatCampaign(naming: String): String {
            return naming.replace("||", "&").replace("()", "=")
        }






        fun getAppBundle(context: Context): String {
            return context.packageName
        }




    fun rootGet(): Boolean {
        val places = arrayOf(
            "/sbin/", "/system/bin/", "/system/xbin/",
            "/data/local/xbin/", "/data/local/bin/",
            "/system/sd/xbin/", "/system/bin/failsafe/",
            "/data/local/"
        )
        try {
            for (where in places) {
                if (File(where + "su").exists()) return true
            }
        } catch (ignore: Throwable) {
            // workaround crash issue in Lenovo devices
            // issues #857
        }
        return false
    }


    fun adbGet(activity: AppCompatActivity): String{

        return Settings.Global.getString(activity.contentResolver, Settings.Global.ADB_ENABLED)  ?: "null"
    }

    fun String.encode(): String {
        return Base64.encodeToString(this.toByteArray(charset("UTF-8")), Base64.DEFAULT)
    }

    }


