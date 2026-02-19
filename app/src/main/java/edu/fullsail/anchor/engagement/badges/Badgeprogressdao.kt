package edu.fullsail.anchor.engagement.badges

// REQUIRED FOR ROOM BADGE STORAGE
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * REQUIRED FOR ROOM BADGE STORAGE
 * DAO for badge progress persistence.
 * Uses @Upsert so both first-time inserts and subsequent updates
 * work with the same call — no need to check if a row exists first.
 */
@Dao
interface BadgeProgressDao {

    // REQUIRED FOR ROOM BADGE STORAGE — live stream of all badge progress rows
    @Query("SELECT * FROM badge_progress")
    fun getAllProgress(): Flow<List<BadgeProgressEntity>>

    // REQUIRED FOR ROOM BADGE STORAGE — insert or update a single badge's progress
    @Upsert
    suspend fun upsert(progress: BadgeProgressEntity)

    // REQUIRED FOR ROOM BADGE STORAGE — wipes all rows (useful for reset/debug)
    @Query("DELETE FROM badge_progress")
    suspend fun clearAll()
}