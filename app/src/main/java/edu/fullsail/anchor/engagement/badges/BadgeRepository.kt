package edu.fullsail.anchor.engagement.badges


/*
 Repository interface for managing Badge data.
 This abstraction allows the badge system to:
 - Retrieve the current list of badges
 - Persist updated badge state
 By using an interface, we can:
 - Swap implementations (memory, Room, DataStore, etc.)
 - Unit test badge logic independently of storage
 */
interface BadgeRepository {
    /*
    Returns the current list of badges.
    This should reflect the user's current unlock
    state and progress values.
     */
    fun getBadges(): List<Badge>

    /*
    Persists updated badge data.
    Called after badge progress or unlock state changes.
     */
    fun saveBadges(badges: List<Badge>)
}