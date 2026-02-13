package edu.fullsail.anchor.engagement.badges

/**
 * Simple in-memory implementation of [BadgeRepository].
 *
 * This repository:
 * - Stores badge data only in RAM
 * - Resets when the app process is killed
 * - Is useful for development and testing
 *
 * This should later be replaced with a persistent
 * implementation (Room, DataStore, etc.) for production.
 */
class InMemoryBadgeRepository : BadgeRepository {
    /**
     * Backing storage for badges.
     *
     * Initialized with default locked badge definitions
     * from the BadgeRuleEngine.
     */
    private var badges: List<Badge> = BadgeRuleEngine.initialBadges()
    /**
     * Returns the current in-memory badge list.
     *
     * Since this is not persisted, it reflects only
     * changes made during this app session.
     */
    override fun getBadges(): List<Badge> = badges
    /**
     * Replaces the current badge list with updated data.
     *
     * This does NOT persist data across app restarts.
     */
    override fun saveBadges(updated: List<Badge>) {
        badges = updated
    }
}