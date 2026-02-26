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

// =============================================================================
// SETTINGS SCREEN
// A scrollable list of all user-configurable preferences. Organized into sections:
//   - Task Behavior  (confirm delete, compact mode)
//   - Focus Mode     (limit Focus section to 3 tasks)
//   - Task Defaults  (default timeframe and priority for new tasks)
//   - Priority Screen (hide low-priority tasks)
//   - Theme          (System/Light/Dark)           [not this developer's code]
//   - Accessibility  (color vision profiles)       [not this developer's code]
//   - Notifications  (enable/disable reminders)
// =============================================================================
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

        // --- Task Behavior ---
        // Controls how tasks behave when the user takes destructive or display actions.
        Text(
            text     = "Task Behavior",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // When ON, deleting a task opens a confirmation dialog before it is removed.
        // When OFF, the task is deleted immediately without prompting.
        SettingItem(
            title          = "Confirm before deleting",
            description    = "Show confirmation dialog when deleting tasks",
            checked        = settings.confirmBeforeDeleting,
            onCheckedChange = { settingsViewModel.updateConfirmBeforeDeleting(it) }
        )

        Divider()

        // When ON, task card padding is reduced so more items fit on screen at once.
        SettingItem(
            title          = "Compact Mode",
            description    = "Reduce spacing in task rows to fit more on screen",
            checked        = settings.compactMode,
            onCheckedChange = { settingsViewModel.updateCompactMode(it) }
        )

        Divider()

        // --- Focus Mode ---
        // Controls how many tasks appear in the Focus (High Priority) section of the Priority screen.
        Text(
            text     = "Focus Mode",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // When ON, only the first 3 high-priority tasks are shown in the Focus section.
        // A "+N additional tasks" label in the section header tells the user how many are hidden.
        SettingItem(
            title          = "Limit Focus Tasks to 3",
            description    = "Show only the first 3 high-priority tasks in Focus section",
            checked        = settings.limitFocusToThree,
            onCheckedChange = { settingsViewModel.updateLimitFocusToThree(it) }
        )

        Divider()

        // --- Task Defaults ---
        // Pre-selects values in the Create Task screen so users don't have to set them every time.
        Text(
            text     = "Task Defaults",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Sets which timeframe radio button is pre-selected when opening the Create Task screen.
        DropdownSettingItem(
            title         = "Default Timeframe",
            description   = "Preset for new tasks — None leaves timeframe unassigned",
            options       = listOf("None", "Daily", "Weekly", "Monthly", "Yearly"),
            selectedValue = settings.defaultTimeframe,
            onValueChange = { settingsViewModel.updateDefaultTimeframe(it) }
        )

        Divider()

        // Sets which priority radio button is pre-selected when opening the Create Task screen.
        DropdownSettingItem(
            title         = "Default Priority",
            description   = "Preset for new tasks — None leaves priority unassigned",
            options       = listOf("None", "High", "Medium", "Low"),
            selectedValue = settings.defaultPriority,
            onValueChange = { settingsViewModel.updateDefaultPriority(it) }
        )

        Divider()

        // --- Smart Sorting ---
        // Controls the order in which active tasks appear within their sections on the Tasks screen.
        // "Manual" preserves drag-and-drop order; other modes apply automatic sorting.
        Text(
            text     = "Smart Sorting",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        DropdownSettingItem(
            title         = "Sort Tasks By",
            description   = "Manual preserves your drag-and-drop order",
            options       = listOf("Manual", "By Priority", "By Due Date", "Soonest First"),
            selectedValue = settings.sortMode,
            onValueChange = { settingsViewModel.updateSortMode(it) }
        )

        Divider()

        // --- Priority Screen ---
        // Controls what the Priority screen displays.
        Text(
            text     = "Priority Screen",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // When ON, the Later/Optional (Low Priority) section is hidden from the Priority screen
        // so users can focus on high and medium priority work without distraction.
        SettingItem(
            title          = "Hide Low Priority Tasks",
            description    = "Don't show low priority tasks in Priority screen",
            checked        = settings.hideLowPriorityInPriorityScreen,
            onCheckedChange = { settingsViewModel.updateHideLowPriorityInPriorityScreen(it) }
        )

        Divider()

        // --- Theme --- (implemented by separate team member)
        Text(
            text     = "Theme",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        ThemeSelector(
            selectedMode   = settings.themeMode,
            onModeSelected = { settingsViewModel.setThemeMode(it) }
        )

        Divider()

        // --- Accessibility --- (implemented by separate team member)
        Text(
            text     = "Accessibility",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text     = "Color vision adjustment",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ColorProfileSelector(
            selectedProfile   = settings.colorProfile,
            onProfileSelected = { settingsViewModel.setColorProfile(it) }
        )

        Divider()

        // --- Notifications ---
        // Controls whether the app is permitted to send task reminder notifications.
        Text(
            text     = "Notifications",
            style    = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        SettingItem(
            title          = "Enable Notifications",
            description    = "Allow Anchor to send reminders and alerts",
            checked        = settings.notificationsEnabled,
            onCheckedChange = { settingsViewModel.updateNotificationsEnabled(it) }
        )
    }
}

// Theme radio button selector — implemented by separate team member
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
                        onClick  = { onModeSelected(option) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedMode == option),
                    onClick  = null  // click handled by the Row's selectable modifier
                )
                Text(
                    text     = option,
                    style    = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// Color-blind mode selector — implemented by separate team member
@Composable
private fun ColorProfileSelector(
    selectedProfile: String,
    onProfileSelected: (String) -> Unit
) {
    data class ProfileOption(val value: String, val label: String, val description: String)

    val options = listOf(
        ProfileOption("Default",      "Default",      "Standard color palette"),
        ProfileOption("Deuteranopia", "Deuteranopia", "Red-green (green-weak)"),
        ProfileOption("Protanopia",   "Protanopia",   "Red-green (red-weak)"),
        ProfileOption("Tritanopia",   "Tritanopia",   "Blue-yellow weakness")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedProfile == option.value),
                        onClick  = { onProfileSelected(option.value) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedProfile == option.value),
                    onClick  = null  // click handled by the Row's selectable modifier
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text  = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =============================================================================
// SETTING ITEM
// Reusable row composable that pairs a title + description label on the left
// with a toggle Switch on the right. Used for all boolean settings.
// =============================================================================
@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// =============================================================================
// DROPDOWN SETTING ITEM
// Reusable row composable that pairs a title + description label on the left
// with a dropdown OutlinedButton on the right. Used for settings that have a
// fixed list of string options (e.g. Default Timeframe, Default Priority).
// =============================================================================
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Button shows the currently selected value and opens the dropdown on tap
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedValue)
                }
                DropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text    = { Text(option) },
                            onClick = { onValueChange(option); expanded = false }
                        )
                    }
                }
            }
        }
    }
}