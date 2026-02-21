package edu.fullsail.anchor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import edu.fullsail.anchor.data.SettingsDataStore
import kotlinx.coroutines.flow.combine

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore? = null
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    init {
        if (settingsDataStore != null) {
            viewModelScope.launch {
                combine(
                    settingsDataStore.themeModeFlow,
                    settingsDataStore.colorProfileFlow,
                    settingsDataStore.confirmBeforeDeletingFlow,
                    settingsDataStore.compactModeFlow,
                    settingsDataStore.limitFocusToThreeFlow
                ) { themeMode, colorProfile, confirmBeforeDeleting, compactMode, limitFocusToThree ->
                    // Return partial — we'll combine remaining below
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
            viewModelScope.launch {
                combine(
                    settingsDataStore.defaultTimeframeFlow,
                    settingsDataStore.defaultPriorityFlow,
                    settingsDataStore.hideLowPriorityFlow
                ) { defaultTimeframe, defaultPriority, hideLowPriority ->
                    Triple(defaultTimeframe, defaultPriority, hideLowPriority)
                }.collect { (defaultTimeframe, defaultPriority, hideLowPriority) ->
                    _settings.value = _settings.value.copy(
                        defaultTimeframe                = defaultTimeframe,
                        defaultPriority                 = defaultPriority,
                        hideLowPriorityInPriorityScreen = hideLowPriority
                    )
                }
            }
        }
    }

    fun updateConfirmBeforeDeleting(value: Boolean) {
        _settings.value = _settings.value.copy(confirmBeforeDeleting = value)
        viewModelScope.launch { settingsDataStore?.saveConfirmBeforeDeleting(value) }
    }

    fun updateCompactMode(value: Boolean) {
        _settings.value = _settings.value.copy(compactMode = value)
        viewModelScope.launch { settingsDataStore?.saveCompactMode(value) }
    }

    fun updateLimitFocusToThree(value: Boolean) {
        _settings.value = _settings.value.copy(limitFocusToThree = value)
        viewModelScope.launch { settingsDataStore?.saveLimitFocusToThree(value) }
    }

    fun updateDefaultTimeframe(value: String) {
        _settings.value = _settings.value.copy(defaultTimeframe = value)
        viewModelScope.launch { settingsDataStore?.saveDefaultTimeframe(value) }
    }

    fun updateDefaultPriority(value: String) {
        _settings.value = _settings.value.copy(defaultPriority = value)
        viewModelScope.launch { settingsDataStore?.saveDefaultPriority(value) }
    }

    fun updateHideLowPriorityInPriorityScreen(value: Boolean) {
        _settings.value = _settings.value.copy(hideLowPriorityInPriorityScreen = value)
        viewModelScope.launch { settingsDataStore?.saveHideLowPriority(value) }
    }

    fun setThemeMode(mode: String) {
        _settings.value = _settings.value.copy(themeMode = mode)
        viewModelScope.launch { settingsDataStore?.saveThemeMode(mode) }
    }

    fun setColorProfile(value: String) {
        _settings.value = _settings.value.copy(colorProfile = value)
        viewModelScope.launch { settingsDataStore?.saveColorProfile(value) }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }
}

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