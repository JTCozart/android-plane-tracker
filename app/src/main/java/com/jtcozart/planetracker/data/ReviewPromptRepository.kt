package com.jtcozart.planetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.reviewPromptDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_prompt")

/** Tracks whether the "rate us" prompt has been shown/handled and when the user first launched the app. */
class ReviewPromptRepository(private val context: Context) {

    val shouldShowPrompt: Flow<Boolean> = context.reviewPromptDataStore.data.map { p ->
        val handled = p[K_PROMPT_HANDLED] ?: false
        val firstLaunch = p[K_FIRST_LAUNCH_AT]
        !handled && firstLaunch != null &&
            System.currentTimeMillis() - firstLaunch >= PROMPT_DELAY_MS
    }

    suspend fun recordFirstLaunchIfNeeded() {
        context.reviewPromptDataStore.edit { p ->
            if (p[K_FIRST_LAUNCH_AT] == null) p[K_FIRST_LAUNCH_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setPromptHandled() {
        context.reviewPromptDataStore.edit { p -> p[K_PROMPT_HANDLED] = true }
    }

    private companion object {
        val K_FIRST_LAUNCH_AT = longPreferencesKey("first_launch_at")
        val K_PROMPT_HANDLED = booleanPreferencesKey("prompt_handled")
        val PROMPT_DELAY_MS = TimeUnit.DAYS.toMillis(1)
    }
}
