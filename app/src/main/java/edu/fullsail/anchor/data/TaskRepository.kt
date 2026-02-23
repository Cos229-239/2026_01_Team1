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
     * Live stream of all tasks as domain models.
     * The Flow from Room automatically emits a new list whenever the
     * database changes, which keeps the UI in sync without manual refresh.
     */
    val tasks: Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        // ADDED FOR PERSISTENCE — convert each entity to domain Task
        entities.map { it.toDomain() }
    }

    // ADDED FOR PERSISTENCE — inserts a new task into the database
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    // ADDED FOR PERSISTENCE — updates an existing task in the database
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    // ADDED FOR PERSISTENCE — deletes a task from the database by its entity representation
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}