package edu.fullsail.anchor.engagement.badges

class InMemoryBadgeRepository : BadgeRepository {
    private var badges: List<Badge> = BadgeRuleEngine.initialBadges()
    override fun getBadges(): List<Badge> = badges
    override fun saveBadges(updated: List<Badge>) {
        badges = updated
    }
}