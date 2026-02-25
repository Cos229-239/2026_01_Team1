package edu.fullsail.anchor

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDateMillis: Long?,
    val priority: String,
    val timeframe: String,
    val isCompleted: Boolean = false,
    // Timestamp used for streak + daily badge calculation (do not remove without updating badge logic)
    val completedAtMillis: Long? = null,
    // ADDED FOR DRAG & DROP — persisted position index within active tasks; lower = earlier in list.
    // New tasks are given Int.MAX_VALUE and appear at the end until a reorder assigns real indices.
    val sortOrder: Int = Int.MAX_VALUE,
    // ADDED FOR SUBTASKS — inline checklist items owned by this task.
    // Persisted as JSON in TaskEntity.subtasksJson via TaskTypeConverters.
    val subtasks: List<Subtask> = emptyList()
) {
    val dueDate: String
        get() {
            if (dueDateMillis == null) return ""
            val today      = Calendar.getInstance().atStartOfDay()
            val dueDateCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }.atStartOfDay()
            val diff = dueDateCal.timeInMillis - today.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diff)
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
        // ADDED FOR COMPLETED SECTION — formats completedAtMillis as "Jan 15" for the completed row label
        val completedDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    }
}

private fun Calendar.atStartOfDay(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}