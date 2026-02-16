package edu.fullsail.anchor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.fullsail.anchor.SettingsViewModel

// Settings screen UI with toggle switches
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel
) {
    val settings by settingsViewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Task Behavior",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Confirm before deleting
        SettingItem(
            title = "Confirm before deleting",
            description = "Show confirmation dialog when deleting tasks",
            checked = settings.confirmBeforeDeleting,
            onCheckedChange = { settingsViewModel.updateConfirmBeforeDeleting(it) }
        )

        Divider()

        // Compact Mode
        SettingItem(
            title = "Compact Mode",
            description = "Reduce spacing in task rows to fit more on screen",
            checked = settings.compactMode,
            onCheckedChange = { settingsViewModel.updateCompactMode(it) }
        )

        Divider()

        Text(
            text = "Focus Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Limit Focus Tasks to 3
        SettingItem(
            title = "Limit Focus Tasks to 3",
            description = "Show only the first 3 high-priority tasks in Focus section",
            checked = settings.limitFocusToThree,
            onCheckedChange = { settingsViewModel.updateLimitFocusToThree(it) }
        )

        Divider()

        // New Settings Section
        Text(
            text = "Task Defaults",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Default Timeframe
        DropdownSettingItem(
            title = "Default Timeframe",
            description = "Timeframe preset when creating new tasks",
            options = listOf("Daily", "Weekly", "Monthly", "Yearly"),
            selectedValue = settings.defaultTimeframe,
            onValueChange = { settingsViewModel.updateDefaultTimeframe(it) }
        )

        Divider()

        // Default Priority
        DropdownSettingItem(
            title = "Default Priority",
            description = "Priority preset when creating new tasks",
            options = listOf("High", "Medium", "Low"),
            selectedValue = settings.defaultPriority,
            onValueChange = { settingsViewModel.updateDefaultPriority(it) }
        )

        Divider()

        Text(
            text = "Priority Screen",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Hide Low Priority Tasks
        SettingItem(
            title = "Hide Low Priority Tasks",
            description = "Don't show low priority tasks in Priority screen",
            checked = settings.hideLowPriorityInPriorityScreen,
            onCheckedChange = { settingsViewModel.updateHideLowPriorityInPriorityScreen(it) }
        )

        Divider()

        // Theme section
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        ThemeSelector(
            selectedMode = settings.themeMode,
            onModeSelected = { settingsViewModel.setThemeMode(it) }
        )

        Divider()

        // Accessibility section
        Text(
            text = "Accessibility",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Color vision adjustment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ColorProfileSelector(
            selectedProfile = settings.colorProfile,
            onProfileSelected = { settingsViewModel.setColorProfile(it) }
        )
    }
}

// Theme radio button selector
// Options kept in a list so Color Blind Mode variants can be added in the same pattern later.
@Composable
private fun ThemeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val themeOptions = listOf("System", "Light", "Dark")

    Column(modifier = Modifier.fillMaxWidth()) {
        themeOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedMode == option),
                        onClick = { onModeSelected(option) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedMode == option),
                    onClick = null  // handled by Row selectable
                )
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// Color blind mode selector — same radio pattern as ThemeSelector.
// Labels include a short description so users know what each mode does.
@Composable
private fun ColorProfileSelector(
    selectedProfile: String,
    onProfileSelected: (String) -> Unit
) {
    data class ProfileOption(val value: String, val label: String, val description: String)

    val options = listOf(
        ProfileOption("Default",       "Default",       "Standard color palette"),
        ProfileOption("Deuteranopia",  "Deuteranopia",  "Red-green (green-weak)"),
        ProfileOption("Protanopia",    "Protanopia",    "Red-green (red-weak)"),
        ProfileOption("Tritanopia",    "Tritanopia",    "Blue-yellow weakness")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedProfile == option.value),
                        onClick = { onProfileSelected(option.value) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedProfile == option.value),
                    onClick = null  // handled by Row selectable
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Reusable setting item with switch
@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Reusable dropdown setting item
@Composable
private fun DropdownSettingItem(
    title: String,
    description: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedValue)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}