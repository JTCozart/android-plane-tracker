package com.jtcozart.planetracker

import android.app.Application
import com.jtcozart.planetracker.notify.NotificationChannels
import org.osmdroid.config.Configuration

class PlaneTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.register(this)

        // osmdroid needs a config + a non-default user agent before any MapView is created.
        Configuration.getInstance().apply {
            load(this@PlaneTrackerApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }
}
