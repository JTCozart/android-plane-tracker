package com.jtcozart.planetracker.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import java.util.Calendar

/** Schedules a once-daily, mid-afternoon check of whether the user has logged a spot today. */
object StreakReminderScheduler {

    private const val REMINDER_HOUR = 14 // 2pm local time

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = reminderPendingIntent(context)

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        // Inexact repeating alarm: no SCHEDULE_EXACT_ALARM permission needed, and a reminder
        // firing a bit early/late doesn't matter for this use case.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            triggerTime.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StreakReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
