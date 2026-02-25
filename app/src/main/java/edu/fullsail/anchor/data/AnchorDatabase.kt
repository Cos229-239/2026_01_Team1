package edu.fullsail.anchor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.fullsail.anchor.engagement.badges.BadgeProgressDao
import edu.fullsail.anchor.engagement.badges.BadgeProgressEntity

/**
 * ADDED FOR PERSISTENCE
 * The main Room database for the Anchor app.
 * Registers TaskEntity and exposes TaskDao.
 * Uses a singleton pattern to avoid multiple database instances.
 *
 * Schema history:
 *   v1 — tasks table
 *   v2 — badge_progress table added (MIGRATION_1_2)
 *   v3 — tasks.sortOrder + tasks.subtasksJson added (MIGRATION_2_3)
 */
@Database(
    entities = [
        TaskEntity::class,
        // REQUIRED FOR ROOM BADGE STORAGE — added BadgeProgressEntity to database
        BadgeProgressEntity::class
    ],
    version = 3,
    exportSchema = false
)
// ADDED FOR SUBTASK PERSISTENCE — registers JSON converter for List<Subtask>
@TypeConverters(TaskTypeConverters::class)
abstract class AnchorDatabase : RoomDatabase() {

    // ADDED FOR PERSISTENCE — provides access to task queries
    abstract fun taskDao(): TaskDao

    // REQUIRED FOR ROOM BADGE STORAGE — provides access to badge progress queries
    abstract fun badgeProgressDao(): BadgeProgressDao

    companion object {

        // ADDED FOR PERSISTENCE — volatile ensures visibility across threads
        @Volatile
        private var INSTANCE: AnchorDatabase? = null

        /**
         * ADDED FOR PERSISTENCE
         * Returns the singleton database instance, creating it if needed.
         * Call this from Application or MainActivity to obtain the db reference.
         */
        fun getInstance(context: Context): AnchorDatabase {
            // ADDED FOR PERSISTENCE — double-checked locking for thread safety
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorDatabase::class.java,
                    "anchor_database"
                )
                    // MODIFIED FOR MIGRATIONS — replaced fallbackToDestructiveMigration() with
                    // explicit migrations so all task data survives schema upgrades.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MIGRATIONS
// ---------------------------------------------------------------------------

// v1 → v2: creates the badge_progress table
private val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) {
    it.execSQL("""
        CREATE TABLE IF NOT EXISTS badge_progress (
            badgeId TEXT NOT NULL PRIMARY KEY,
            progress INTEGER NOT NULL,
            unlocked INTEGER NOT NULL,
            unlockedAtMillis INTEGER,
            timesEarned INTEGER NOT NULL
        )
    """.trimIndent())
}

// v2 → v3: adds sortOrder and subtasksJson columns to the tasks table.
//   sortOrder is seeded from each row's rowid so existing tasks keep their
//   insertion order after the migration — no arbitrary reshuffling.
//   subtasksJson defaults to '[]' so existing rows parse as having no subtasks.
private val MIGRATION_2_3 = androidx.room.migration.Migration(2, 3) {
    it.execSQL("ALTER TABLE tasks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
    it.execSQL("UPDATE tasks SET sortOrder = rowid")
    it.execSQL("ALTER TABLE tasks ADD COLUMN subtasksJson TEXT NOT NULL DEFAULT '[]'")
}