package com.doyouone.drawai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val PRIMARY_COLOR_KEY = longPreferencesKey("primary_color")
        private val IS_FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
        private val IS_TUTORIAL_COMPLETED_KEY = booleanPreferencesKey("is_tutorial_completed")
    }
    
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }
    
    val primaryColorArgb: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PRIMARY_COLOR_KEY]
    }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH_KEY] ?: true
    }
    
    val isTutorialCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_TUTORIAL_COMPLETED_KEY] ?: false // Default false, tutorial not done
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun toggleDarkMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[DARK_MODE_KEY] ?: false
            preferences[DARK_MODE_KEY] = !current
        }
    }
    
    suspend fun setPrimaryColor(colorArgb: Long) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_COLOR_KEY] = colorArgb
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH_KEY] = false
        }
    }
    
    suspend fun setTutorialCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_TUTORIAL_COMPLETED_KEY] = true
        }
    }
    
    suspend fun resetTutorial() {
        context.dataStore.edit { preferences ->
            preferences[IS_TUTORIAL_COMPLETED_KEY] = false
        }
    }
}
