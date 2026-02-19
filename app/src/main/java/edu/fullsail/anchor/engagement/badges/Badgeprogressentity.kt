package edu.fullsail.anchor.engagement.badges

// REQUIRED FOR ROOM BADGE STORAGE
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * REQUIRED FOR ROOM BADGE STORAGE
 * Room entity that stores only the mutable progress state of a badge.
 * The immutable display fields (title, description, iconRes, etc.) stay in
 * BadgeRuleEngine.initialBadges() — this table only persists what changes.
 *
 * Separated from Badge.kt intentionally — do NOT merge these.
 * Badge is the domain/UI model; BadgeProgressEntity is the storage model.
 */
@Entity(tableName = "badge_progress")
data class BadgeProgressEntity(

    // REQUIRED FOR ROOM BADGE STORAGE — matches Badge.id / BadgeIds constants
    @PrimaryKey
    val badgeId: String,

    // REQUIRED FOR ROOM BADGE STORAGE
    // Stored as Int (0–100) to avoid floating-point precision issues in SQLite.
    // Convert to Float via / 100f when mapping back to Badge.progress.
    val progress: Int,

    // REQUIRED FOR ROOM BADGE STORAGE — true when badge has been earned
    val unlocked: Boolean,

    // REQUIRED FOR ROOM BADGE STORAGE — epoch millis when first unlocked, null if not yet earned
    val unlockedAtMillis: Long?,

    // REQUIRED FOR ROOM BADGE STORAGE — how many times this badge has been earned
    val timesEarned: Int
)