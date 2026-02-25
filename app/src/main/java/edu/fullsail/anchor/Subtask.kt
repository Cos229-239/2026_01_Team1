package edu.fullsail.anchor

import java.util.UUID

/**
 * Lightweight subtask model for inline task checklists.
 *
 * Stored as JSON inside the parent TaskEntity row (subtasksJson column) via
 * TaskTypeConverters — no separate table needed.
 *
 * Owned by parent Task: deleting a task deletes all its subtasks.
 * The UI suggests completing the parent when all subtasks are done, but does not force it.
 */
data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)