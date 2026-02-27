package edu.fullsail.anchor

import java.util.UUID

/**
 * Lightweight model for a single checklist item within a parent task.
 *
 * Subtasks are stored as a JSON array inside the parent TaskEntity's subtasksJson
 * column (serialized by TaskTypeConverters) — no separate database table is needed.
 *
 * Ownership: a subtask belongs to its parent task. Deleting the parent task also
 * removes all of its subtasks automatically.
 *
 * The UI suggests completing the parent task when all subtasks are done,
 * but does not force it — the user decides.
 */
data class Subtask(
    // Unique identifier generated automatically at creation time
    val id: String = UUID.randomUUID().toString(),

    // The subtask description entered by the user
    val title: String,

    // True when the user has checked this subtask off
    val isDone: Boolean = false
)