package edu.fullsail.anchor.engagement.badges

import edu.fullsail.anchor.R
import edu.fullsail.anchor.engagement.UserEngagementStats


/**
 * Central rule engine responsible for:
 * - Defining initial badge states
 * - Recomputing badge progress
 * - Determining newly unlocked badges
 *
 * This object contains pure logic only.
 * No UI and no persistence should live here.
 */
object BadgeRuleEngine {
    /**
     * Defines the initial badge list.
     *
     * All badges start locked with 0 progress.
     * These definitions act as the "source of truth"
     * for badge requirements.
     */
    fun initialBadges(): List<Badge> = listOf(
        Badge(
            id= BadgeIds.FIRST_TASK,
            title = "Anchor",
            description = "You completed your first task.",
            requirementText = "Complete 1 task",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_anchor
        ),
        Badge(
            id= BadgeIds.STREAK_3,
            title = "Flame",
            description = "You kept your momentum going.",
            requirementText = "Complete 1 task per day for 3 days",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_flame
        ),
        Badge(
            id= BadgeIds.STEADFAST,
            title = "Checkmark",
            description = "You cleared everything for the day.",
            requirementText = "Complete all tasks scheduled for one day",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_checkmark
        ),
        Badge(
            id= BadgeIds.ADVANCEMENT,
            title = "Double Chevron",
            description = "You made steady progress over time..",
            requirementText = "Complete 25 tasks total",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_chevron
        ),
        Badge(
            id= BadgeIds.EXCEPTIONAL_WEEK,
            title = "Star",
            description = "You showed up every day.",
            requirementText = "Complete at least 1 task per day for 7 days",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_star
        ),
        Badge(
            id= BadgeIds.TEN_IN_A_DAY,
            title = "Shooting Star",
            description = "You had a high-output day.",
            requirementText = "Complete 10 or more tasks in a single day",
            unlocked = false,
            progress = 0f,
            iconRes = R.drawable.badge_shooting_star
        )
    )
    /**
     * Recomputes all badge states based on updated user engagement stats.
     *
     * Returns:
     *  - First: Updated list of badges
     *  - Second: List of badges newly unlocked during this evaluation
     *
     * This method is deterministic:
     * Given the same stats and existing badges,
     * it will always return the same result.
     */
    fun evaluate(
        stats: UserEngagementStats,
        existing: List<Badge>
    ): Pair<List<Badge>, List<Badge>> {

        //Recalculate progress for every badge
        val updated = existing.map {
            badge -> when(badge.id){
            BadgeIds.FIRST_TASK -> applyRule(badge, progress(stats.completedTasksTotal, goal = 1))
            BadgeIds.STREAK_3 -> applyRule(badge, progress(stats.streakDays, goal = 3))
            BadgeIds.STEADFAST -> {
                // Badge unlocks only if all scheduled tasks were completed
                val cleared = stats.scheduledToday > 0 &&
                        stats.completedToday >= stats.scheduledToday
                applyRule(badge, if(cleared) 1f else 0f)
            }
            BadgeIds.ADVANCEMENT -> applyRule(badge, progress(stats.completedTasksTotal, goal = 25))
            BadgeIds.EXCEPTIONAL_WEEK -> applyRule(badge, progress(stats.streakDays, goal = 7))
            BadgeIds.TEN_IN_A_DAY -> applyRule(badge, progress(stats.completedToday, goal = 10))
            else -> badge
            }
        }
        /**
         * Detects badges that were previously locked
         * but are now unlocked during this evaluation cycle.
         *
         * Useful for:
         * - Showing celebration dialogs
         * - Triggering animations
         * - Sending notifications
         */
        val newlyUnlocked = updated.filter { u ->
            u.unlocked && (existing.firstOrNull { it.id == u.id}?.unlocked == false)
        }
        return updated to newlyUnlocked
    }
    /**
     * Calculates progress toward a goal as a float (0f–1f).
     *
     * coerceIn ensures:
     * - Progress never exceeds 1f
     * - Progress never drops below 0f
     */
    private fun progress(value: Int, goal: Int): Float{
        if(goal <= 0) return 0f
        return(value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    }
    /**
     * Applies a calculated progress value to a badge.
     *
     * If progress reaches 1f, the badge becomes unlocked.
     * Once unlocked, progress is forced to 1f.
     */
    private fun applyRule(badge: Badge, p: Float): Badge {
        val unlocked = p >= 1f
        return badge.copy(
            unlocked = unlocked,
            progress = if(unlocked) 1f else p
        )
    }
}
/**
 * Centralized badge ID constants.
 *
 * Using constants prevents:
 * - Typos
 * - Hardcoded strings
 * - Rule mismatches
 *
 * These IDs act as stable references across the app.
 */
object BadgeIds {
    const val FIRST_TASK = "first_task"
    const val STREAK_3 = "streak_3"
    const val STEADFAST = "steadfast"
    const val ADVANCEMENT = "advancement"
    const val EXCEPTIONAL_WEEK = "exceptional_week"
    const val TEN_IN_A_DAY = "ten_in_a_day"
}

