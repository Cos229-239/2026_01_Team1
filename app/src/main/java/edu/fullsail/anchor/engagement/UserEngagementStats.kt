package edu.fullsail.anchor.engagement

/**
 * Aggregated user engagement metrics used by the BadgeRuleEngine.
 *
 * This data class acts as a read-only snapshot of user activity.
 * It contains only the information necessary for badge evaluation.
 *
 * Important:
 * - This class should not contain business logic.
 * - It should be derived from your task system/database.
 * - It keeps the badge system decoupled from task implementation details.
 */
data class UserEngagementStats(
    /**
     * Total number of tasks completed across the lifetime of the user.
     *
     * Used for:
     * - First task badge
     * - Advancement badge (25 tasks)
     */
    val completedTasksTotal: Int = 0,
    /**
     * Number of tasks scheduled for today.
     *
     * Used for:
     * - Determining if all scheduled tasks were completed (STEADFAST badge)
     */
    val scheduledToday: Int = 0,

    /**
     * Number of tasks completed today.
     *
     * Used for:
     * - TEN_IN_A_DAY badge
     * - Daily completion checks
     */
    val completedToday: Int = 0,
    /**
     * Current consecutive-day streak of completing at least one task.
     *
     * Used for:
     * - STREAK_3 badge
     * - EXCEPTIONAL_WEEK badge
     */
    val streakDays: Int = 0
)
