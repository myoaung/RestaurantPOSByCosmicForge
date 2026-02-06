package com.cosmicforge.rms.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for application settings persistence
 * v10.4.2: Owner Controls & Feature Flags
 * 
 * Uses DataStore for thread-safe preference management
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val KEY_GAMIFICATION_ENABLED = booleanPreferencesKey("gamification_enabled")
        private val KEY_PUBLIC_RECOGNITION_ENABLED = booleanPreferencesKey("public_recognition_enabled")
    }
    
    /**
     * Observe gamification feature flag
     */
    val isGamificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_GAMIFICATION_ENABLED] ?: true // Default: enabled
        }
    
    /**
     * Observe public recognition feature flag
     */
    val isPublicRecognitionEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_PUBLIC_RECOGNITION_ENABLED] ?: true // Default: enabled
        }
    
    /**
     * Toggle gamification (Owner only)
     */
    suspend fun setGamificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAMIFICATION_ENABLED] = enabled
        }
    }
    
    /**
     * Toggle public recognition (Owner only)
     */
    suspend fun setPublicRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PUBLIC_RECOGNITION_ENABLED] = enabled
        }
    }
}
