package edu.fullsail.anchor

// =============================================================================
// APP SETTINGS
// Immutable data class that holds every user-configurable preference in one place.
// SettingsViewModel exposes this as a StateFlow so the UI reacts whenever any
// value changes. Default values here also serve as the app's initial state before
// any persisted preferences have been loaded from SettingsDataStore.
// =============================================================================
data class AppSettings(

    // ---- Task Behavior ----
    // When true, deleting a task opens a confirmation dialog before removal.
    val confirmBeforeDeleting: Boolean = true,

    // When true, task card padding is reduced so more items fit on screen at once.
    val compactMode: Boolean = false,

    // ---- Focus Mode ----
    // When true, the Focus section on the Priority screen shows at most 3 tasks.
    val limitFocusToThree: Boolean = true,

    // ---- Task Defaults ----
    // Pre-selected timeframe shown on the Create Task screen for new tasks.
    // "None" means no timeframe is pre-selected — task lands in the Unassigned section.
    val defaultTimeframe: String = "None",

    // Pre-selected priority shown on the Create Task screen for new tasks.
    // "None" means no priority is pre-selected — task lands in the Unassigned section on the Priority screen.
    val defaultPriority: String = "None",

    // ---- Smart Sorting ----
    // Controls how active tasks are ordered within sections on the Tasks screen.
    //   "Manual"        — drag-and-drop order (user's explicit arrangement, default)
    //   "By Priority"   — High > Medium > Low > Unassigned within each timeframe section
    //   "By Due Date"   — ascending due date within each section (no date goes last)
    //   "Soonest First" — all tasks merged into one flat list sorted by due date
    val sortMode: String = "Manual",

    // ---- Priority Screen ----
    // When true, the Later/Optional (Low Priority) section is hidden on the Priority screen.
    val hideLowPriorityInPriorityScreen: Boolean = false,

    // ---- Theme ---- (implemented by separate team member)
    // Controls light/dark mode. Values: "System", "Light", "Dark"
    val themeMode: String = "System",

    // Controls the color palette for color vision accessibility.
    // Branch on this in AnchorTheme to swap color schemes.
    val colorProfile: String = "Default",

    // ---- Notifications ----
    // When true, the app is allowed to send task reminder notifications.
    val notificationsEnabled: Boolean = true
)