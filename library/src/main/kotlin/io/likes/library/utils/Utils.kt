package io.likes.library.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
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


        val isDeviceRooted: Boolean
            get() = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

        private fun checkRootMethod1(): Boolean {
            val buildTags = Build.TAGS
            return buildTags != null && buildTags.contains("test-keys")
        }

        private fun checkRootMethod2(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            return false
        }

        private fun checkRootMethod3(): Boolean {
            var process: Process? = null
            return try {
                process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
                val `in` = BufferedReader(InputStreamReader(process.inputStream))
                `in`.readLine() != null
            } catch (t: Throwable) {
                false
            } finally {
                process?.destroy()
            }
        }

        fun isDevMode(activity: AppCompatActivity): Boolean {
            return when {
                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN -> {
                    Settings.Secure.getInt(activity.contentResolver,
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN -> {
                    @Suppress("DEPRECATION")
                    Settings.Secure.getInt(activity.contentResolver,
                        Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
                }
                else -> false
            }
        }

    }


