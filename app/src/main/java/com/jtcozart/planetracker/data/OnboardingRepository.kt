package com.jtcozart.planetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

/** Tracks whether the user has completed (or skipped) the first-launch tutorial. */
class OnboardingRepository(private val context: Context) {

    val tutorialCompleted: Flow<Boolean> =
        context.onboardingDataStore.data.map { p -> p[K_TUTORIAL_COMPLETED] ?: false }

    suspend fun setTutorialCompleted() {
        context.onboardingDataStore.edit { p -> p[K_TUTORIAL_COMPLETED] = true }
    }

    private companion object {
        val K_TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
    }
}
