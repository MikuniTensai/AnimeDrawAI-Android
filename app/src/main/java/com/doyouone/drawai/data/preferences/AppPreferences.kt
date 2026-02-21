package com.doyouone.drawai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * AppPreferences - Manages app-level preferences
 */
class AppPreferences(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")
        
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save_enabled")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_GALLERY_LOCK = booleanPreferencesKey("gallery_lock_enabled")
        private val KEY_RESTRICTED_CONTENT = booleanPreferencesKey("restricted_content_enabled")
        private val KEY_AGE_VERIFIED = booleanPreferencesKey("age_verified")
        private val KEY_GALLERY_PIN = stringPreferencesKey("gallery_pin")
        private val KEY_REMINDER_HOUR = androidx.datastore.preferences.core.intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = androidx.datastore.preferences.core.intPreferencesKey("reminder_minute")
    }
    
    /**
     * Auto-save enabled state
     */
    val isAutoSaveEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SAVE] ?: true // Default: enabled
    }
    
    /**
     * Set auto-save enabled
     */
    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SAVE] = enabled
        }
    }
    
    /**
     * Notifications enabled state
     */
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS] ?: true // Default: enabled
    }
    
    /**
     * Set notifications enabled
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS] = enabled
        }
    }
    
    /**
     * Gallery lock enabled state
     */
    val isGalleryLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_GALLERY_LOCK] ?: false
    }
    
    /**
     * Set gallery lock enabled
     */
    suspend fun setGalleryLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GALLERY_LOCK] = enabled
        }
    }
    
    /**
     * Gallery PIN
     */
    val galleryPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_GALLERY_PIN]
    }
    
    /**
     * Set gallery PIN
     */
    suspend fun setGalleryPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GALLERY_PIN] = pin
        }
    }
    
    /**
     * Restricted content enabled state
     */
    val isRestrictedContentEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RESTRICTED_CONTENT] ?: false
    }
    
    /**
     * Set restricted content enabled
     */
    suspend fun setRestrictedContentEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RESTRICTED_CONTENT] = enabled
        }
    }
    
    /**
     * Age verified state
     */
    val isAgeVerified: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AGE_VERIFIED] ?: false
    }
    
    /**
     * Set age verified
     */
    suspend fun setAgeVerified(verified: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AGE_VERIFIED] = verified
        }
    }
    
    /**
     * Reminder Time
     */
    val reminderHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_HOUR] ?: 8
    }
    
    val reminderMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_MINUTE] ?: 0
    }
    
    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    private val KEY_ACTIVE_REMINDERS = androidx.datastore.preferences.core.stringSetPreferencesKey("active_reminders_chars")

    val activeReminderCharacters: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_REMINDERS] ?: emptySet()
    }

    suspend fun addActiveReminderCharacter(name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ACTIVE_REMINDERS] ?: emptySet()
            preferences[KEY_ACTIVE_REMINDERS] = current + name
        }
    }

    suspend fun removeActiveReminderCharacter(name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ACTIVE_REMINDERS] ?: emptySet()
            preferences[KEY_ACTIVE_REMINDERS] = current - name
        }
    }
}
