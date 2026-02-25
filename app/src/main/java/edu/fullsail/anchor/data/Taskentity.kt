package edu.fullsail.anchor.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.fullsail.anchor.Subtask
import edu.fullsail.anchor.Task
import org.json.JSONArray
import org.json.JSONObject

/**
 * ADDED FOR PERSISTENCE
 * Room entity that mirrors the Task domain model field-for-field.
 * The original Task class is NOT removed — this is the database layer only.
 * Mapper functions at the bottom convert between the two representations.
 *
 * Schema v3 changes (MIGRATION_2_3):
 *   sortOrder    INTEGER NOT NULL DEFAULT 0    — for drag & drop reordering
 *   subtasksJson TEXT    NOT NULL DEFAULT '[]' — for inline subtask checklists
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    // ADDED FOR PERSISTENCE — matches Task.id (UUID string)
    @PrimaryKey
    val id: String,

    // ADDED FOR PERSISTENCE — matches Task.title
    val title: String,

    // ADDED FOR PERSISTENCE — matches Task.dueDateMillis (nullable Long)
    val dueDateMillis: Long?,

    // ADDED FOR PERSISTENCE — matches Task.priority
    val priority: String,

    // ADDED FOR PERSISTENCE — matches Task.timeframe
    val timeframe: String,

    // ADDED FOR PERSISTENCE — matches Task.isCompleted
    val isCompleted: Boolean,

    // ADDED FOR PERSISTENCE — matches Task.completedAtMillis
    // Timestamp used for streak + daily badge calculation (do not remove without updating badge logic)
    val completedAtMillis: Long?,
    // ADDED FOR DRAG & DROP — seeded from rowid in MIGRATION_2_3 so existing tasks keep their order
    val sortOrder: Int = 0,
    // ADDED FOR SUBTASKS — JSON-serialised List<Subtask>; defaults to empty array
    val subtasksJson: String = "[]"
)

// ADDED FOR PERSISTENCE
// Converts a Room entity to the domain Task model used by the UI and ViewModels
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

// ADDED FOR PERSISTENCE
// Converts a domain Task model to a Room entity for database storage
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

// Inline JSON helpers — mirror TaskTypeConverters so mappers are self-contained
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
    } catch (e: Exception) { emptyList() }
}