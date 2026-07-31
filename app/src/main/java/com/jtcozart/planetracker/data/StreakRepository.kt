package com.jtcozart.planetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

private val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(name = "streak")

data class StreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveEpochDay: Long? = null,
) {
    /** True if the current streak was already recorded today, i.e. it can't be lost by closing the app now. */
    val securedToday: Boolean
        get() = lastActiveEpochDay == LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}

/** Tracks consecutive days the user has opened the app with tracking active. */
class StreakRepository(private val context: Context) {

    val streak: Flow<StreakState> = context.streakDataStore.data.map { p ->
        StreakState(
            currentStreak = p[K_CURRENT] ?: 0,
            longestStreak = p[K_LONGEST] ?: 0,
            lastActiveEpochDay = p[K_LAST_ACTIVE_DAY],
        )
    }

    suspend fun current(): StreakState = streak.first()

    /** Call whenever the user logs a spot. No-ops if today is already recorded (streak already secured). */
    suspend fun recordSpotToday() {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        context.streakDataStore.edit { p ->
            val lastActive = p[K_LAST_ACTIVE_DAY]
            if (lastActive == today) return@edit

            val newStreak = if (lastActive == today - 1) (p[K_CURRENT] ?: 0) + 1 else 1
            p[K_CURRENT] = newStreak
            p[K_LONGEST] = maxOf(p[K_LONGEST] ?: 0, newStreak)
            p[K_LAST_ACTIVE_DAY] = today
        }
    }

    private companion object {
        val K_CURRENT = intPreferencesKey("current_streak")
        val K_LONGEST = intPreferencesKey("longest_streak")
        val K_LAST_ACTIVE_DAY = longPreferencesKey("last_active_epoch_day")
    }
}
