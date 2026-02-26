package edu.fullsail.anchor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TaskEntity.
 *
 * getAllTasks() returns a Flow so the UI automatically recomposes whenever the
 * task list changes in Room — no manual refresh calls are needed.
 *
 * All write operations are suspend functions so they must be called from a
 * coroutine (typically via viewModelScope.launch in TaskViewModel).
 */
@Dao
interface TaskDao {

    /**
     * Returns all tasks ordered by sortOrder ascending.
     * Ordering by sortOrder ensures drag-and-drop changes are reflected immediately
     * after TaskRepository.reorderTasks() updates the values.
     * New tasks with sortOrder = Int.MAX_VALUE naturally appear at the bottom.
     */
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * Inserts a new task. REPLACE conflict strategy means re-inserting a task with
     * the same ID will overwrite the existing row — effectively an upsert.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    /** Updates all fields of an existing task matched by its primary key (id). */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /** Deletes a task from the database by matching its primary key (id). */
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    /**
     * Updates only the sortOrder column for a single task.
     * Called in a loop by TaskRepository.reorderTasks() after a drag-and-drop
     * gesture completes, assigning sequential indices (0, 1, 2, ...) to all tasks.
     */
    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}