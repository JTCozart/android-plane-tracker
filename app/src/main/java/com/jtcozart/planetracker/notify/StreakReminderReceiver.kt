package com.jtcozart.planetracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jtcozart.planetracker.data.SettingsRepository
import com.jtcozart.planetracker.data.StreakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Fires once a day; posts a reminder only if the user hasn't already secured today's streak. */
class StreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settings.first()
                if (!settings.notifyStreakReminder) return@launch

                val streak = StreakRepository(context).current()
                if (!streak.securedToday) {
                    Notifier(context).notifyStreakReminder(streak.currentStreak)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Reschedules the daily reminder alarm after a reboot, since repeating alarms don't survive one. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StreakReminderScheduler.schedule(context)
        }
    }
}
