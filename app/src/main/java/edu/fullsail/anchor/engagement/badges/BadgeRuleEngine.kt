package edu.fullsail.anchor.engagement.badges

import edu.fullsail.anchor.R

object BadgeRuleEngine {
    /*
    Initial badge definitions (locked)
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
    /*
    Recomputes all badge progress/unlocked states from stats
     */
    fun evaluate(
        stats: UserEngagementStats,
        existing: List<Badge>
    ): Pair<List<Badge>, List<Badge>> {
        val updated = existing.map {
            badge -> when(badge.id){
            BadgeIds.FIRST_TASK -> applyRule(badge, progress(stats.completedTasksTotal, goal = 1))
            BadgeIds.STREAK_3 -> applyRule(badge, progress(stats.streakDays, goal = 3))
            BadgeIds.STEADFAST -> {
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
        val newlyUnlocked = updated.filter { u ->
            u.unlocked && (existing.firstOrNull { it.id == u.id}?.unlocked == false)
        }
        return updated to newlyUnlocked
    }
    private fun progress(value: Int, goal: Int): Float{
        if(goal <= 0) return 0f
        return(value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    }
    private fun applyRule(badge: Badge, p: Float): Badge {
        val unlocked = p >= 1f
        return badge.copy(
            unlocked = unlocked,
            progress = if(unlocked) 1f else p
        )
    }
}
/*
IDs are const to reference badges safely
 */
object BadgeIds {
    const val FIRST_TASK = "first_task"
    const val STREAK_3 = "streak_3"
    const val STEADFAST = "steadfast"
    const val ADVANCEMENT = "advancement"
    const val EXCEPTIONAL_WEEK = "exceptional_week"
    const val TEN_IN_A_DAY = "ten_in_a_day"
}
/*
Stats that drive badge progress
NEED TO ATTACH REAL TASK SYSTEM
 */
data class UserEngagementStats(
    val completedTasksTotal: Int = 0,
    val scheduledToday: Int = 0,
    val completedToday: Int = 0,
    val streakDays: Int = 0
)