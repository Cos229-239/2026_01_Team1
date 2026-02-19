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

    // ADDED FOR PERSISTENCE — returns a live stream of all tasks, ordered by insertion
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // ADDED FOR PERSISTENCE — inserts a new task; replaces on conflict (handles upsert for updates)
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    // ADDED FOR PERSISTENCE — updates an existing task matched by primary key (id)
    @Update
    suspend fun updateTask(task: TaskEntity)

    // ADDED FOR PERSISTENCE — deletes a task matched by primary key (id)
    @Delete
    suspend fun deleteTask(task: TaskEntity)
}