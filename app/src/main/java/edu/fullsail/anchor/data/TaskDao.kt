package edu.fullsail.anchor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * ADDED FOR PERSISTENCE
 * Data Access Object for TaskEntity.
 * Uses Flow for getAllTasks() so the UI automatically updates when data changes.
 * All write operations are suspend functions to be called from coroutines.
 */
@Dao
interface TaskDao {

    // MODIFIED FOR DRAG & DROP — now orders by sortOrder ASC so user-reordered tasks
    // appear in the correct sequence. New tasks with sortOrder = Int.MAX_VALUE appear last.
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // ADDED FOR DRAG & DROP REORDERING — updates sortOrder for one task by id.
    // Called in a loop from TaskRepository.reorderTasks() after a drag gesture completes.
    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}