package edu.fullsail.anchor.data

import edu.fullsail.anchor.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that sits between Room (TaskDao) and the TaskViewModel.
 *
 * Exposes domain Task objects rather than TaskEntity objects, so every layer
 * above the repository is completely unaware of the database schema. All
 * entity-to-domain conversion happens here using the mapper functions in
 * TaskEntity.kt.
 *
 * This is the only class that should ever call TaskDao directly.
 */
class TaskRepository(private val taskDao: TaskDao) {

    /**
     * Live stream of all tasks as domain models, ordered by sortOrder.
     * Room's Flow emits a new list automatically whenever any task changes,
     * so the UI stays in sync without polling or manual refreshes.
     */
    val tasks: Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    /** Inserts a new task into Room. Converts to entity before writing. */
    suspend fun insertTask(task: Task) = taskDao.insertTask(task.toEntity())

    /** Updates all fields of an existing task. Converts to entity before writing. */
    suspend fun updateTask(task: Task) = taskDao.updateTask(task.toEntity())

    /** Deletes a task from Room. */
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task.toEntity())

    /**
     * Persists a new task order after a drag-and-drop gesture completes.
     * [orderedTasks] is the full active (non-completed) task list in the desired order.
     * Assigns each task a sequential sortOrder (0, 1, 2, ...) so the new order
     * is preserved when the app restarts and the list is read from Room.
     */
    suspend fun reorderTasks(orderedTasks: List<Task>) {
        orderedTasks.forEachIndexed { index, task ->
            taskDao.updateSortOrder(task.id, index)
        }
    }
}