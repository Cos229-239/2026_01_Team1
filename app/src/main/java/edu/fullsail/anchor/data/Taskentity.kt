package edu.fullsail.anchor.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.fullsail.anchor.Subtask
import edu.fullsail.anchor.Task
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room database entity for the "tasks" table.
 *
 * This class mirrors the Task domain model field-for-field but exists as a separate
 * class so the rest of the app remains unaware of the database layer. All conversion
 * between TaskEntity and Task happens through the mapper functions at the bottom of
 * this file (toDomain() and toEntity()).
 *
 * Schema history:
 *   v1 — id, title, dueDateMillis, priority, timeframe, isCompleted, completedAtMillis
 *   v3 — sortOrder (MIGRATION_2_3), subtasksJson (MIGRATION_2_3)
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    // UUID string — matches Task.id, serves as the Room primary key
    @PrimaryKey
    val id: String,

    val title: String,

    // Nullable epoch millis — null means the task has no due date
    val dueDateMillis: Long?,

    // "High", "Medium", or "Low"
    val priority: String,

    // "Daily", "Weekly", "Monthly", or "Yearly"
    val timeframe: String,

    val isCompleted: Boolean,

    // Epoch millis when the task was completed; null when active or unchecked.
    // Used for streak and badge calculations — do not remove without updating badge logic.
    val completedAtMillis: Long?,

    // Position index for drag-and-drop ordering. Seeded from rowid in MIGRATION_2_3
    // so existing tasks keep their insertion order after the schema upgrade.
    val sortOrder: Int = 0,

    // JSON-serialised List<Subtask>. Defaults to "[]" so existing rows before v3
    // have no subtasks rather than a null parse error.
    val subtasksJson: String = "[]"
)

// =============================================================================
// MAPPER FUNCTIONS
// Convert between the storage model (TaskEntity) and the domain model (Task).
// These are extension functions so they read naturally at the call site:
//   entity.toDomain()   — used in TaskRepository when reading from Room
//   task.toEntity()     — used in TaskRepository when writing to Room
// =============================================================================

/** Converts a Room entity to the domain Task model used by the UI and ViewModels. */
fun TaskEntity.toDomain(): Task = Task(
    id                = id,
    title             = title,
    dueDateMillis     = dueDateMillis,
    priority          = priority,
    timeframe         = timeframe,
    isCompleted       = isCompleted,
    completedAtMillis = completedAtMillis,
    sortOrder         = sortOrder,
    subtasks          = parseSubtasksJson(subtasksJson)
)

/** Converts a domain Task model to a Room entity ready for database storage. */
fun Task.toEntity(): TaskEntity = TaskEntity(
    id                = id,
    title             = title,
    dueDateMillis     = dueDateMillis,
    priority          = priority,
    timeframe         = timeframe,
    isCompleted       = isCompleted,
    completedAtMillis = completedAtMillis,
    sortOrder         = sortOrder,
    subtasksJson      = serializeSubtasksJson(subtasks)
)

// =============================================================================
// JSON HELPERS
// These functions mirror the logic in TaskTypeConverters so the mapper functions
// above are self-contained and don't depend on Room's @TypeConverters at the
// entity mapping layer.
// =============================================================================

private fun serializeSubtasksJson(subtasks: List<Subtask>): String {
    val array = JSONArray()
    subtasks.forEach { s ->
        val obj = JSONObject()
        obj.put("id",     s.id)
        obj.put("title",  s.title)
        obj.put("isDone", s.isDone)
        array.put(obj)
    }
    return array.toString()
}

private fun parseSubtasksJson(json: String): List<Subtask> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Subtask(
                id     = obj.getString("id"),
                title  = obj.getString("title"),
                isDone = obj.getBoolean("isDone")
            )
        }
    } catch (e: Exception) {
        emptyList() // Return empty list rather than crashing if JSON is malformed
    }
}