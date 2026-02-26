package edu.fullsail.anchor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.fullsail.anchor.engagement.badges.BadgeProgressDao
import edu.fullsail.anchor.engagement.badges.BadgeProgressEntity

/**
 * The main Room database for the Anchor app.
 *
 * Registers all entities and exposes their DAOs. Uses the singleton pattern
 * (double-checked locking) to ensure only one database instance exists at a time,
 * which is required by Room.
 *
 * Schema version history:
 *   v1 — tasks table (id, title, dueDateMillis, priority, timeframe, isCompleted, completedAtMillis)
 *   v2 — badge_progress table added (MIGRATION_1_2)
 *   v3 — tasks.sortOrder and tasks.subtasksJson columns added (MIGRATION_2_3)
 *
 * Explicit migrations are used (rather than fallbackToDestructiveMigration) so that
 * existing user data is preserved when the schema is updated.
 */
@Database(
    entities = [
        TaskEntity::class,
        BadgeProgressEntity::class  // REQUIRED FOR ROOM BADGE STORAGE
    ],
    version = 3,
    exportSchema = false
)
// Registers TaskTypeConverters so Room can serialize/deserialize List<Subtask>
@TypeConverters(TaskTypeConverters::class)
abstract class AnchorDatabase : RoomDatabase() {

    /** Provides access to all task CRUD and reorder queries. */
    abstract fun taskDao(): TaskDao

    /** Provides access to badge progress persistence queries. */
    abstract fun badgeProgressDao(): BadgeProgressDao

    companion object {

        // @Volatile ensures every thread sees the most up-to-date value of INSTANCE
        @Volatile
        private var INSTANCE: AnchorDatabase? = null

        /**
         * Returns the singleton database instance, creating it if it doesn't exist yet.
         * The synchronized block with double-checked locking prevents two threads from
         * both seeing INSTANCE as null and each creating their own database object.
         */
        fun getInstance(context: Context): AnchorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorDatabase::class.java,
                    "anchor_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// =============================================================================
// MIGRATIONS
// Each migration runs inside a transaction provided by Room. Only DDL statements
// that the specific version change requires should be added here.
// =============================================================================

/**
 * v1 → v2: Creates the badge_progress table for storing badge unlock state.
 * Added when the badge persistence system was introduced.
 */
private val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) {
    it.execSQL(
        """
        CREATE TABLE IF NOT EXISTS badge_progress (
            badgeId          TEXT    NOT NULL PRIMARY KEY,
            progress         INTEGER NOT NULL,
            unlocked         INTEGER NOT NULL,
            unlockedAtMillis INTEGER,
            timesEarned      INTEGER NOT NULL
        )
        """.trimIndent()
    )
}

/**
 * v2 → v3: Adds drag-and-drop ordering and subtask support to the tasks table.
 *
 * sortOrder is seeded from each row's rowid so existing tasks keep their insertion
 * order after the migration rather than all being assigned 0.
 *
 * subtasksJson defaults to '[]' so every existing row is treated as having no
 * subtasks, which parses cleanly in TaskTypeConverters.toSubtaskList().
 */
private val MIGRATION_2_3 = androidx.room.migration.Migration(2, 3) {
    it.execSQL("ALTER TABLE tasks ADD COLUMN sortOrder    INTEGER NOT NULL DEFAULT 0")
    it.execSQL("UPDATE tasks SET sortOrder = rowid")
    it.execSQL("ALTER TABLE tasks ADD COLUMN subtasksJson TEXT    NOT NULL DEFAULT '[]'")
}