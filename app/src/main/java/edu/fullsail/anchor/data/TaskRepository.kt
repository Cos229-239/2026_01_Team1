package edu.fullsail.anchor.data

import edu.fullsail.anchor.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ADDED FOR PERSISTENCE
 * Repository that sits between the Room database and the TaskViewModel.
 * Exposes domain Task objects (not entities) so the rest of the app is
 * unaware of the database layer. All conversion happens here using the
 * mapper functions defined in TaskEntity.kt.
 */
class TaskRepository(private val taskDao: TaskDao) {

    /**
     * ADDED FOR PERSISTENCE
     * Live stream of all tasks as domain models, ordered by sortOrder (see TaskDao).
     * The Flow from Room automatically emits a new list whenever the
     * database changes, keeping the UI in sync without manual refresh.
     */
    val tasks: Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insertTask(task: Task) = taskDao.insertTask(task.toEntity())
    suspend fun updateTask(task: Task) = taskDao.updateTask(task.toEntity())
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task.toEntity())

    /**
     * ADDED FOR DRAG & DROP REORDERING
     * Persists a new task order to Room after a drag-and-drop gesture completes.
     * [orderedTasks] is the full active (non-completed) task list in the user's
     * desired order. Each task receives a sequential sortOrder (0, 1, 2, ...).
     */
    suspend fun reorderTasks(orderedTasks: List<Task>) {
        orderedTasks.forEachIndexed { index, task ->
            taskDao.updateSortOrder(task.id, index)
        }
    }
}