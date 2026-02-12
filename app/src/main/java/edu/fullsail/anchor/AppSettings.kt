package edu.fullsail.anchor

// Data model for app-wide settings
data class AppSettings(

    val confirmBeforeDeleting: Boolean = true,  // Default: ON - show confirmation dialog
    val compactMode: Boolean = false,           // Default: OFF - normal spacing
    val limitFocusToThree: Boolean = true,      // Default: ON - limit Focus section to 3 tasks
    val defaultTimeframe: String = "Daily",     // Default timeframe for new tasks
    val defaultPriority: String = "Medium",     // Default priority for new tasks
    val hideLowPriorityInPriorityScreen: Boolean = false,  // Hide low priority tasks in Priority screen

    // Theme switching — values: "System", "Light", "Dark"
    val themeMode: String = "System",

    // Future: Color Blind Mode — no UI yet
    // Branch on this in AnchorTheme to swap color schemes when implemented.
    val colorProfile: String = "Default"
)