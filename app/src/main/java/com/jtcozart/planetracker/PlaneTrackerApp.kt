package com.jtcozart.planetracker

import android.app.Application
import android.util.Log
import androidx.work.Configuration as WorkConfiguration
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.jtcozart.planetracker.notify.NotificationChannels
import com.jtcozart.planetracker.notify.StreakReminderScheduler
import org.osmdroid.config.Configuration

class PlaneTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.register(this)
        MobileAds.initialize(this)
        initWorkManager()
        StreakReminderScheduler.schedule(this)

        // osmdroid needs a config + a non-default user agent before any MapView is created.
        Configuration.getInstance().apply {
            load(this@PlaneTrackerApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }

    /**
     * WorkManager (pulled in transitively by the Ads SDK) normally self-initializes via its
     * androidx.startup ContentProvider, but that's disabled in the manifest so we can recover
     * here: if the on-disk database is from an incompatible older schema — which otherwise
     * crashes the whole app on startup for anyone upgrading in place — delete just that
     * database and retry, rather than losing all the user's settings/history.
     */
    private fun initWorkManager() {
        val config = WorkConfiguration.Builder().build()
        try {
            WorkManager.initialize(this, config)
        } catch (e: Exception) {
            Log.w("PlaneTrackerApp", "WorkManager init failed, resetting its database", e)
            deleteDatabase("androidx.work.workdb")
            try {
                WorkManager.initialize(this, config)
            } catch (e2: Exception) {
                Log.e("PlaneTrackerApp", "WorkManager init failed even after reset; continuing without it", e2)
            }
        }
    }
}
