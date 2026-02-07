package edu.fullsail.anchor.engagement


data class UserEngagementStats(
    val completedTasksTotal: Int = 0,
    val scheduledToday: Int = 0,
    val completedToday: Int = 0,
    val streakDays: Int = 0
)
