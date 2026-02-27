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

// Creates a single DataStore instance scoped to the Context (typically the Application).
// The "anchor_settings" name is the filename for the on-disk preferences file.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "anchor_settings"
)

/**
 * Persistent key-value store for all user settings using Jetpack DataStore.
 *
 * Each setting has a typed Preferences key (defined in the companion object),
 * a read-only Flow that emits the current value with a sensible default, and
 * a suspend save function that writes an updated value to disk.
 *
 * SettingsViewModel subscribes to these Flows so the UI reacts to preference
 * changes without any additional wiring. The DataStore writes are off the main
 * thread automatically, so save functions can be called from any coroutine.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        // ---- Theme keys ---- (managed by separate team member)
        val THEME_MODE     = stringPreferencesKey("theme_mode")
        val COLOR_PROFILE  = stringPreferencesKey("color_profile")

        // ---- Task Behavior keys ----
        val CONFIRM_BEFORE_DELETING = booleanPreferencesKey("confirm_before_deleting")
        val COMPACT_MODE            = booleanPreferencesKey("compact_mode")

        // ---- Focus Mode key ----
        val LIMIT_FOCUS_TO_THREE = booleanPreferencesKey("limit_focus_to_three")

        // ---- Task Default keys ----
        val DEFAULT_TIMEFRAME = stringPreferencesKey("default_timeframe")
        val DEFAULT_PRIORITY  = stringPreferencesKey("default_priority")

        // ---- Priority Screen key ----
        val HIDE_LOW_PRIORITY = booleanPreferencesKey("hide_low_priority_in_priority_screen")
    }

    // ---- Read Flows ----
    // Each Flow emits the persisted value or its default if no value has been saved yet.

    val themeModeFlow: Flow<String>          = context.dataStore.data.map { it[THEME_MODE]               ?: "System" }
    val colorProfileFlow: Flow<String>       = context.dataStore.data.map { it[COLOR_PROFILE]            ?: "Default" }
    val confirmBeforeDeletingFlow: Flow<Boolean> = context.dataStore.data.map { it[CONFIRM_BEFORE_DELETING] ?: true }
    val compactModeFlow: Flow<Boolean>       = context.dataStore.data.map { it[COMPACT_MODE]             ?: false }
    val limitFocusToThreeFlow: Flow<Boolean> = context.dataStore.data.map { it[LIMIT_FOCUS_TO_THREE]     ?: true }
    val defaultTimeframeFlow: Flow<String>   = context.dataStore.data.map { it[DEFAULT_TIMEFRAME]        ?: "Daily" }
    val defaultPriorityFlow: Flow<String>    = context.dataStore.data.map { it[DEFAULT_PRIORITY]         ?: "Medium" }
    val hideLowPriorityFlow: Flow<Boolean>   = context.dataStore.data.map { it[HIDE_LOW_PRIORITY]        ?: false }

    // ---- Write Functions ----
    // All writes use dataStore.edit() which runs on the IO dispatcher automatically.

    suspend fun saveThemeMode(mode: String)             { context.dataStore.edit { it[THEME_MODE]               = mode    } }
    suspend fun saveColorProfile(profile: String)       { context.dataStore.edit { it[COLOR_PROFILE]            = profile } }
    suspend fun saveConfirmBeforeDeleting(value: Boolean) { context.dataStore.edit { it[CONFIRM_BEFORE_DELETING] = value   } }
    suspend fun saveCompactMode(value: Boolean)         { context.dataStore.edit { it[COMPACT_MODE]             = value   } }
    suspend fun saveLimitFocusToThree(value: Boolean)   { context.dataStore.edit { it[LIMIT_FOCUS_TO_THREE]     = value   } }
    suspend fun saveDefaultTimeframe(value: String)     { context.dataStore.edit { it[DEFAULT_TIMEFRAME]        = value   } }
    suspend fun saveDefaultPriority(value: String)      { context.dataStore.edit { it[DEFAULT_PRIORITY]         = value   } }
    suspend fun saveHideLowPriority(value: Boolean)     { context.dataStore.edit { it[HIDE_LOW_PRIORITY]        = value   } }
}