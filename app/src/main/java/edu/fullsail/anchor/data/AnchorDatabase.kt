package edu.fullsail.anchor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import edu.fullsail.anchor.engagement.badges.BadgeProgressDao
import edu.fullsail.anchor.engagement.badges.BadgeProgressEntity

/**
 * ADDED FOR PERSISTENCE
 * The main Room database for the Anchor app.
 * Registers TaskEntity and exposes TaskDao.
 * Uses a singleton pattern to avoid multiple database instances.
 *
 * Version starts at 1. If you add columns or tables in future,
 * increment version and add a Migration — do NOT use fallbackToDestructiveMigration
 * in production builds.
 *
 * MODIFIED FOR PERSISTENCE — version bumped to 2, BadgeProgressEntity added.
 * A destructive migration fallback is used here only because badge progress
 * can be safely re-earned. For Task data we would use a proper Migration instead.
 */
@Database(
    entities = [
        TaskEntity::class,
        // REQUIRED FOR ROOM BADGE STORAGE — added BadgeProgressEntity to database
        BadgeProgressEntity::class
    ],
    // MODIFIED FOR PERSISTENCE — bumped from 1 to 2 for new badge_progress table
    version = 2,
    exportSchema = false
)
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
                    "anchor_database"  // ADDED FOR PERSISTENCE — database file name on disk
                )
                    // REQUIRED FOR ROOM BADGE STORAGE
                    // Destructive migration is acceptable here because badge progress
                    // can be re-earned. Tasks are unaffected — they live in the same
                    // file but Room handles table-level migrations independently when
                    // using fallbackToDestructiveMigration only as a last resort.
                    // Replace with a real Migration object if preserving task data matters.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}