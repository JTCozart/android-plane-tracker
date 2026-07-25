package com.jtcozart.planetracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jtcozart.planetracker.MainActivity
import com.jtcozart.planetracker.R
import com.jtcozart.planetracker.data.AdsbLolSource
import com.jtcozart.planetracker.data.AircraftStore
import com.jtcozart.planetracker.data.FetchResult
import com.jtcozart.planetracker.data.HistoryRepository
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.SettingsRepository
import com.jtcozart.planetracker.data.TrackerStateHolder
import com.jtcozart.planetracker.location.LocationProvider
import com.jtcozart.planetracker.model.AircraftClass
import com.jtcozart.planetracker.notify.NotificationChannels
import com.jtcozart.planetracker.notify.Notifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.firstOrNull

/**
 * Foreground service that polls adsb.lol using the phone's live location and posts
 * notifications. This is the Android equivalent of the firmware's setup()/loop().
 */
class TrackingService : LifecycleService() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var historyRepo: HistoryRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var notifier: Notifier
    private val source = AdsbLolSource()
    private val store = AircraftStore()

    private val currentSettings = MutableStateFlow(Settings())
    private val refreshNow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    @Volatile private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        historyRepo = HistoryRepository(applicationContext)
        locationProvider = LocationProvider(applicationContext)
        notifier = Notifier(applicationContext)

        startForegroundCompat()

        // Load persisted history into the store before polling begins.
        lifecycleScope.launch {
            val saved = historyRepo.history.firstOrNull() ?: emptyList()
            if (saved.isNotEmpty()) {
                store.loadHistory(saved)
                TrackerStateHolder.update { it.copy(history = store.historyAircraft) }
            }
        }

        // Observe clear-history signal from the ViewModel.
        lifecycleScope.launch {
            TrackerStateHolder.clearHistorySignal.collect {
                store.clearHistory()
            }
        }

        // Keep latest settings; on any change push new radius to state and re-poll immediately.
        lifecycleScope.launch {
            var first = true
            settingsRepo.settings.collect { settings ->
                val prev = currentSettings.value
                currentSettings.value = settings
                TrackerStateHolder.update { it.copy(radiusNm = settings.radiusNm) }
                if (!first && settings.radiusNm != prev.radiusNm) refreshNow.tryEmit(Unit)
                first = false
            }
        }
        // Track location — radius follows the phone. Update interval = half the poll interval.
        lifecycleScope.launch {
            val intervalMs = (currentSettings.value.pollIntervalSec * 1000L).coerceAtLeast(5000L)
            locationProvider.locationUpdates(intervalMs).collect { loc ->
                if (!TrackerStateHolder.state.value.locationLocked) {
                    lastLocation = loc
                    TrackerStateHolder.update {
                        it.copy(centerLat = loc.latitude, centerLon = loc.longitude)
                    }
                }
            }
        }
        // Poll loop.
        lifecycleScope.launch { pollLoop() }

        TrackerStateHolder.update { it.copy(running = true) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        TrackerStateHolder.update { it.copy(running = false) }
        super.onDestroy()
    }

    private suspend fun pollLoop() {
        while (true) {
            val settings = currentSettings.value
            val loc = lastLocation
            if (loc != null) poll(loc, settings)
            val delayMs = settings.pollIntervalSec.coerceAtLeast(Settings.MIN_POLL_INTERVAL_SEC) * 1000L
            withTimeoutOrNull(delayMs) { refreshNow.first() }
        }
    }

    private suspend fun poll(loc: Location, settings: Settings) {
        when (val result = source.fetch(loc.latitude, loc.longitude, settings.radiusNm)) {
            is FetchResult.Success -> {
                val effects = store.ingest(result.response, settings)
                effects.newDetections.forEach { notifier.notifyDetection(it, settings) }
                effects.emergencies.forEach { notifier.notifyEmergencySquawk(it, settings) }
                publishState(loc, settings, result.httpCode)
                if (effects.hasNewAircraft) historyRepo.save(store.historyAircraft)
            }
            is FetchResult.Failure -> {
                TrackerStateHolder.update {
                    it.copy(
                        lastHttpCode = result.httpCode,
                        consecutiveFailures = source.consecutiveFailures,
                    )
                }
            }
        }
    }

    private fun publishState(loc: Location, settings: Settings, httpCode: Int) {
        TrackerStateHolder.update { current ->
            current.copy(
                running = true,
                active = store.activeAircraft,
                history = store.historyAircraft,
                counts = AircraftClass.entries.associateWith { c -> store.detectionCount(c) },
                centerLat = if (current.locationLocked) current.centerLat else loc.latitude,
                centerLon = if (current.locationLocked) current.centerLon else loc.longitude,
                radiusNm = settings.radiusNm,
                lastHttpCode = httpCode,
                lastAircraftCount = store.lastAircraftCount,
                consecutiveFailures = source.consecutiveFailures,
                lastUpdateMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun startForegroundCompat() {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, NotificationChannels.SERVICE)
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setContentTitle("PlaneTracker active")
            .setContentText("Scanning for aircraft near you")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val NOTIF_ID = 1
        const val ACTION_STOP = "com.jtcozart.planetracker.STOP"

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}
