package com.v2ray.ang

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.model.UserProfile
import com.v2ray.ang.utilx.SFAppObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlinx.coroutines.*
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
import com.v2ray.ang.BuildConfig
import org.matomo.sdk.TrackMe
import org.matomo.sdk.extra.TrackHelper

class AngApplication : MultiDexApplication() {
    companion object {
        lateinit var application: AngApplication
        lateinit var tracker: Tracker
        private const val TAG = "AngApplication"
    }

    /**
     * Attaches the base context to the application.
     * @param base The base context.
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    /**
     * Initializes the application.
     */
    override fun onCreate() {
        super.onCreate()

        ////----
        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(this.resources.configuration)
        config.setLocale(locale)
        this.createConfigurationContext(config)
        this.resources.updateConfiguration(config, this.resources.displayMetrics)
        ////----

        MMKV.initialize(this)

        SettingsManager.setNightMode()
        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)

        SettingsManager.initRoutingRulesets(this)

        es.dmoral.toasty.Toasty.Config.getInstance()
            .setGravity(android.view.Gravity.BOTTOM, 0, 200)
            .apply()

        // 先从本地存储恢复用户存档
        runBlocking(Dispatchers.IO) {
            UserProfile.readUserFromLocal(this@AngApplication.contentResolver)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(SFAppObserver)

        tracker = TrackerBuilder.createDefault(BuildConfig.MATOMO_TRACKER_URL, BuildConfig.MATOMO_SITE_ID)
            .build(Matomo.getInstance(this))
        tracker.setUserId(UserProfile.user.deviceId)
        tracker.addTrackingCallback { trackMe: TrackMe? ->
            Log.d(TAG, "Tracker.Callback.onTrack(${trackMe})")
            trackMe
        }
        TrackHelper.track().event("c_user", "c_launch").with(tracker)
    }

}
