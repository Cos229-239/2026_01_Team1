package edu.fullsail.anchor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import edu.fullsail.anchor.data.SettingsDataStore
import kotlinx.coroutines.flow.combine

// =============================================================================
// SETTINGS VIEW MODEL
// Holds the single source of truth for all app settings as a StateFlow<AppSettings>.
// On creation, it reads persisted values from SettingsDataStore and merges them into
// the state. Each update function writes changes back to the DataStore so preferences
// survive app restarts.
//
// Two combine() collectors are used because combine() only supports up to 5 flows
// at a time — the settings are split across two groups accordingly.
// =============================================================================
class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore? = null
) : ViewModel() {

    // Starts with sensible defaults (defined in AppSettings) and is updated as
    // the DataStore flows emit their persisted values on startup.
    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    init {
        if (settingsDataStore != null) {

            // Group 1: theme mode, color profile, and the three core task behavior toggles.
            // combine() emits a new value whenever any of the five flows changes.
            viewModelScope.launch {
                combine(
                    settingsDataStore.themeModeFlow,
                    settingsDataStore.colorProfileFlow,
                    settingsDataStore.confirmBeforeDeletingFlow,
                    settingsDataStore.compactModeFlow,
                    settingsDataStore.limitFocusToThreeFlow
                ) { themeMode, colorProfile, confirmBeforeDeleting, compactMode, limitFocusToThree ->
                    // Package results into an array so combine()'s lambda can return them
                    arrayOf(themeMode, colorProfile, confirmBeforeDeleting, compactMode, limitFocusToThree)
                }.collect { partial ->
                    _settings.value = _settings.value.copy(
                        themeMode             = partial[0] as String,
                        colorProfile          = partial[1] as String,
                        confirmBeforeDeleting = partial[2] as Boolean,
                        compactMode           = partial[3] as Boolean,
                        limitFocusToThree     = partial[4] as Boolean
                    )
                }
            }

            // Group 2: task defaults, Priority screen filter, and smart sort mode.
            // combine() only supports up to 5 flows, so sortMode is added to this group
            // (which already had 3 flows) rather than creating a third combine block.
            viewModelScope.launch {
                combine(
                    settingsDataStore.defaultTimeframeFlow,
                    settingsDataStore.defaultPriorityFlow,
                    settingsDataStore.hideLowPriorityFlow,
                    settingsDataStore.sortModeFlow
                ) { values ->
                    // 4-flow combine requires the Array lambda form
                    arrayOf(values[0], values[1], values[2], values[3])
                }.collect { values ->
                    _settings.value = _settings.value.copy(
                        defaultTimeframe                = values[0] as String,
                        defaultPriority                 = values[1] as String,
                        hideLowPriorityInPriorityScreen = values[2] as Boolean,
                        sortMode                        = values[3] as String
                    )
                }
            }
        }
    }

    // --- Task Behavior settings ---

    /** Toggles whether a confirmation dialog is shown before deleting a task. */
    fun updateConfirmBeforeDeleting(value: Boolean) {
        _settings.value = _settings.value.copy(confirmBeforeDeleting = value)
        viewModelScope.launch { settingsDataStore?.saveConfirmBeforeDeleting(value) }
    }

    /** Toggles compact mode, which reduces card padding so more tasks fit on screen. */
    fun updateCompactMode(value: Boolean) {
        _settings.value = _settings.value.copy(compactMode = value)
        viewModelScope.launch { settingsDataStore?.saveCompactMode(value) }
    }

    /** Toggles whether the Focus section on the Priority screen is capped at 3 tasks. */
    fun updateLimitFocusToThree(value: Boolean) {
        _settings.value = _settings.value.copy(limitFocusToThree = value)
        viewModelScope.launch { settingsDataStore?.saveLimitFocusToThree(value) }
    }

    // --- Task Default settings ---
    // These values pre-populate the timeframe and priority fields on the Create Task screen.

    /** Sets the default timeframe (Daily/Weekly/Monthly/Yearly) for new tasks. */
    fun updateDefaultTimeframe(value: String) {
        _settings.value = _settings.value.copy(defaultTimeframe = value)
        viewModelScope.launch { settingsDataStore?.saveDefaultTimeframe(value) }
    }

    /** Sets the default priority (High/Medium/Low) for new tasks. */
    fun updateDefaultPriority(value: String) {
        _settings.value = _settings.value.copy(defaultPriority = value)
        viewModelScope.launch { settingsDataStore?.saveDefaultPriority(value) }
    }

    // --- Priority Screen settings ---

    /** Toggles whether Low priority tasks are hidden on the Priority screen. */
    fun updateHideLowPriorityInPriorityScreen(value: Boolean) {
        _settings.value = _settings.value.copy(hideLowPriorityInPriorityScreen = value)
        viewModelScope.launch { settingsDataStore?.saveHideLowPriority(value) }
    }

    // --- Smart Sorting setting ---

    /** Sets the sort mode for the Tasks screen. Values: "Manual", "By Priority", "By Due Date", "Soonest First". */
    fun updateSortMode(value: String) {
        _settings.value = _settings.value.copy(sortMode = value)
        viewModelScope.launch { settingsDataStore?.saveSortMode(value) }
    }

    // --- Theme settings --- (implemented by separate team member)

    fun setThemeMode(mode: String) {
        _settings.value = _settings.value.copy(themeMode = mode)
        viewModelScope.launch { settingsDataStore?.saveThemeMode(mode) }
    }

    fun setColorProfile(value: String) {
        _settings.value = _settings.value.copy(colorProfile = value)
        viewModelScope.launch { settingsDataStore?.saveColorProfile(value) }
    }

    // --- Notification settings ---

    /**
     * Toggles whether Anchor can send task reminder notifications.
     * The notification channel itself is registered in MainActivity on startup;
     * this flag controls whether the app presents the option to the user.
     */
    fun updateNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }
}

// =============================================================================
// SETTINGS VIEW MODEL FACTORY
// Required because SettingsViewModel takes a SettingsDataStore constructor
// parameter that the default ViewModelProvider cannot supply on its own.
// =============================================================================
class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}