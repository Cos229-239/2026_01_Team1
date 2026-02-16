package edu.fullsail.anchor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel to hold and manage app settings
// In-memory storage using StateFlow
class SettingsViewModel : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    fun updateConfirmBeforeDeleting(value: Boolean) {
        _settings.value = _settings.value.copy(confirmBeforeDeleting = value)
    }

    fun updateCompactMode(value: Boolean) {
        _settings.value = _settings.value.copy(compactMode = value)
    }

    fun updateLimitFocusToThree(value: Boolean) {
        _settings.value = _settings.value.copy(limitFocusToThree = value)
    }

    fun updateDefaultTimeframe(value: String) {
        _settings.value = _settings.value.copy(defaultTimeframe = value)
    }

    fun updateDefaultPriority(value: String) {
        _settings.value = _settings.value.copy(defaultPriority = value)
    }

    fun updateHideLowPriorityInPriorityScreen(value: Boolean) {
        _settings.value = _settings.value.copy(hideLowPriorityInPriorityScreen = value)
    }

    // Theme switching — "System", "Light", "Dark"
    fun setThemeMode(mode: String) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    // Accessibility: Color Blind Mode — "Default", "Deuteranopia", "Protanopia", "Tritanopia"
    fun setColorProfile(value: String) {
        _settings.value = _settings.value.copy(colorProfile = value)
    }
}






