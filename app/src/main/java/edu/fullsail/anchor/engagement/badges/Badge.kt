package edu.fullsail.anchor.engagement.badges

import androidx.annotation.DrawableRes
/*
Rep a sing achievement badge in app
 */
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val requirementText: String,
    val unlocked: Boolean,
    val progress: Float,
    @DrawableRes val iconRes: Int
)

/*
Engagement events that can affect badge progress.
intentionally  generic to be triggered from any screen.
 */
enum class EngagementEventType{
    TASK_COMPLETED,
    STREAK_COMPLETED
}

data class EngagementEvent(
    val type: EngagementEventType,
    val completedTasksTotal: Int? = null,
    val completedToday: Int? = null,
val streakDays: Int? = null
)