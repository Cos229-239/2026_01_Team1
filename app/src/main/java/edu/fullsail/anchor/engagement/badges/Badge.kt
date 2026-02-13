package edu.fullsail.anchor.engagement.badges

import androidx.annotation.DrawableRes
/*
* Represents a single achievement badge in the Anchor app.
 *
 * Each badge tracks:
 * - Basic identity info (id, title, description)
 * - Requirement text shown to the user
 * - Unlock state
 * - Progress toward unlocking (0f–1f)
 * - The drawable resource used for its icon
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
/*
 Represents an engagement event sent to the badge engine.
 Nullable values allow flexibility so only relevant
 data needs to be provided depending on the event type.
 */
data class EngagementEvent(
    val type: EngagementEventType,
    val completedTasksTotal: Int? = null,
    val completedToday: Int? = null,
val streakDays: Int? = null
)