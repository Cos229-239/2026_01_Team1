package edu.fullsail.anchor.data

// ADDED FOR PERSISTENCE
import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.fullsail.anchor.Task

/**
 * ADDED FOR PERSISTENCE
 * Room entity that mirrors the Task domain model field-for-field.
 * The original Task class is NOT removed — this is the database layer only.
 * Mapper functions at the bottom convert between the two representations.
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
    val completedAtMillis: Long?
)

// ADDED FOR PERSISTENCE
// Converts a Room entity to the domain Task model used by the UI and ViewModels
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    dueDateMillis = dueDateMillis,
    priority = priority,
    timeframe = timeframe,
    isCompleted = isCompleted,
    completedAtMillis = completedAtMillis
)

// ADDED FOR PERSISTENCE
// Converts a domain Task model to a Room entity for database storage
fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    dueDateMillis = dueDateMillis,
    priority = priority,
    timeframe = timeframe,
    isCompleted = isCompleted,
    completedAtMillis = completedAtMillis
)