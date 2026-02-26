package edu.fullsail.anchor

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

// =============================================================================
// TASK
// Core domain model representing a single user task. This class is used
// throughout the UI and ViewModels. The database layer stores tasks as
// TaskEntity objects (see TaskEntity.kt) and converts them to/from Task
// using the toDomain() / toEntity() mapper functions.
// =============================================================================
data class Task(
    // Unique identifier — a UUID string generated at creation time
    val id: String = UUID.randomUUID().toString(),

    // The task title entered by the user — required, must not be blank
    val title: String,

    // Optional due date stored as epoch milliseconds; null means no due date
    val dueDateMillis: Long?,

    // Task priority tier: "High", "Medium", or "Low"
    val priority: String,

    // Scheduling category: "Daily", "Weekly", "Monthly", or "Yearly"
    val timeframe: String,

    // True when the user has checked off the task as done
    val isCompleted: Boolean = false,

    // Epoch milliseconds when the task was completed.
    // Used for streak calculation and daily badge progress.
    // Do not remove without updating the badge evaluation pipeline.
    val completedAtMillis: Long? = null,

    // Position index within the active task list, used for drag & drop ordering.
    // New tasks default to Int.MAX_VALUE so they appear at the bottom of the list.
    // Real indices (0, 1, 2, ...) are assigned after the first reorder is saved to Room.
    val sortOrder: Int = Int.MAX_VALUE,

    // Inline subtask checklist. Persisted as a JSON string in TaskEntity.subtasksJson
    // via TaskTypeConverters — no separate database table is needed.
    val subtasks: List<Subtask> = emptyList()
) {
    // Computed human-readable due date label shown under the task title.
    // Returns an empty string when no due date is set.
    val dueDate: String
        get() {
            if (dueDateMillis == null) return ""
            val today      = Calendar.getInstance().atStartOfDay()
            val dueDateCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }.atStartOfDay()
            val diff       = dueDateCal.timeInMillis - today.timeInMillis
            val days       = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                days == 0L -> "Due today at ${timeFormat.format(Date(dueDateMillis))}"
                days == 1L -> "Due tomorrow at ${timeFormat.format(Date(dueDateMillis))}"
                days < 0L  -> "${-days} days overdue"
                else       -> dateFormat.format(Date(dueDateMillis))
            }
        }

    companion object {
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())

        // Formats completedAtMillis as a short date (e.g. "Jan 15") for the completed task label.
        val completedDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    }
}

// Extension function to zero out the time portion of a Calendar so that due date
// comparisons (today / tomorrow / overdue) compare calendar days, not exact timestamps.
private fun Calendar.atStartOfDay(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}