package edu.fullsail.anchor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "anchor_settings"
)

class SettingsDataStore(private val context: Context) {

    companion object {
        // Theme
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PROFILE = stringPreferencesKey("color_profile")

        // Task Behavior
        val CONFIRM_BEFORE_DELETING = booleanPreferencesKey("confirm_before_deleting")
        val COMPACT_MODE = booleanPreferencesKey("compact_mode")

        // Focus Mode
        val LIMIT_FOCUS_TO_THREE = booleanPreferencesKey("limit_focus_to_three")

        // Task Defaults
        val DEFAULT_TIMEFRAME = stringPreferencesKey("default_timeframe")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")

        // Priority Screen
        val HIDE_LOW_PRIORITY = booleanPreferencesKey("hide_low_priority_in_priority_screen")
    }

    // --- Flows ---

    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "System" }
    val colorProfileFlow: Flow<String> = context.dataStore.data.map { it[COLOR_PROFILE] ?: "Default" }
    val confirmBeforeDeletingFlow: Flow<Boolean> = context.dataStore.data.map { it[CONFIRM_BEFORE_DELETING] ?: true }
    val compactModeFlow: Flow<Boolean> = context.dataStore.data.map { it[COMPACT_MODE] ?: false }
    val limitFocusToThreeFlow: Flow<Boolean> = context.dataStore.data.map { it[LIMIT_FOCUS_TO_THREE] ?: true }
    val defaultTimeframeFlow: Flow<String> = context.dataStore.data.map { it[DEFAULT_TIMEFRAME] ?: "Daily" }
    val defaultPriorityFlow: Flow<String> = context.dataStore.data.map { it[DEFAULT_PRIORITY] ?: "Medium" }
    val hideLowPriorityFlow: Flow<Boolean> = context.dataStore.data.map { it[HIDE_LOW_PRIORITY] ?: false }

    // --- Save functions ---

    suspend fun saveThemeMode(mode: String) { context.dataStore.edit { it[THEME_MODE] = mode } }
    suspend fun saveColorProfile(profile: String) { context.dataStore.edit { it[COLOR_PROFILE] = profile } }
    suspend fun saveConfirmBeforeDeleting(value: Boolean) { context.dataStore.edit { it[CONFIRM_BEFORE_DELETING] = value } }
    suspend fun saveCompactMode(value: Boolean) { context.dataStore.edit { it[COMPACT_MODE] = value } }
    suspend fun saveLimitFocusToThree(value: Boolean) { context.dataStore.edit { it[LIMIT_FOCUS_TO_THREE] = value } }
    suspend fun saveDefaultTimeframe(value: String) { context.dataStore.edit { it[DEFAULT_TIMEFRAME] = value } }
    suspend fun saveDefaultPriority(value: String) { context.dataStore.edit { it[DEFAULT_PRIORITY] = value } }
    suspend fun saveHideLowPriority(value: Boolean) { context.dataStore.edit { it[HIDE_LOW_PRIORITY] = value } }
}