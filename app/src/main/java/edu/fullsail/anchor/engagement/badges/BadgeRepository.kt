package edu.fullsail.anchor.engagement.badges

interface BadgeRepository {
    fun getBadges(): List<Badge>
    fun saveBadges(Badges: List<Badge>)
}