package edu.fullsail.anchor.engagement.badges

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.text.get
import kotlin.text.toInt

/**
 * REQUIRED FOR ROOM BADGE STORAGE
 * Replaces InMemoryBadgeRepository as the storage layer for badges.
 * Implements the existing BadgeRepository interface unchanged — the
 * ViewModel and RuleEngine do not need to know about Room at all.
 *
 * Strategy:
 * - getBadges()  → merges initialBadges() from BadgeRuleEngine with
 *                  persisted progress rows from Room
 * - saveBadges() → upserts updated progress rows back into Room
 *
 * getBadges() uses runBlocking intentionally because BadgeRepository
 * is a synchronous interface. If the interface is ever updated to
 * return Flow or suspend, remove runBlocking and use coroutines directly.
 */
class RoomBadgeRepository(
    // REQUIRED FOR ROOM BADGE STORAGE — injected DAO
    private val dao: BadgeProgressDao,
    // REQUIRED FOR ROOM BADGE STORAGE — scope for fire-and-forget writes
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : BadgeRepository {

    /**
     * REQUIRED FOR ROOM BADGE STORAGE
     * Returns badges with persisted progress merged in.
     * Steps:
     * 1. Get the canonical badge list from BadgeRuleEngine (id, title, icon, etc.)
     * 2. Read saved progress rows from Room
     * 3. For each badge, if a progress row exists, apply it; otherwise use defaults
     */
    override fun getBadges(): List<Badge> {
        // REQUIRED FOR ROOM BADGE STORAGE
        // Read saved progress from Room. runBlocking is acceptable here because
        // getBadges() is called from the ViewModel init/refresh, not from the UI thread
        // hot path. Flow.first() collects exactly one emission then completes.
        val savedProgress: List<BadgeProgressEntity> = runBlocking {
            dao.getAllProgress().first()
        }

        // REQUIRED FOR ROOM BADGE STORAGE — build a lookup map for quick access
        val progressMap = savedProgress.associateBy { it.badgeId }

        // REQUIRED FOR ROOM BADGE STORAGE
        // Start from the canonical definition list so titles, icons, and descriptions
        // are always up to date. Only overwrite progress/unlocked from persisted data.
        return BadgeRuleEngine.initialBadges().map { badge ->
            val saved = progressMap[badge.id]
            if (saved != null) {
                // REQUIRED FOR ROOM BADGE STORAGE — apply persisted progress to domain model
                badge.copy(
                    unlocked = saved.unlocked,
                    progress = saved.progress / 100f  // convert Int back to Float
                )
            } else {
                // REQUIRED FOR ROOM BADGE STORAGE — no saved row yet, use locked defaults
                badge
            }
        }
    }

    /**
     * REQUIRED FOR ROOM BADGE STORAGE
     * Persists the updated badge list to Room.
     * Only progress and unlocked state are written — display fields are not stored.
     * Uses fire-and-forget coroutine on IO dispatcher so the UI is never blocked.
     */
    override fun saveBadges(Badges: List<Badge>) {
        // REQUIRED FOR ROOM BADGE STORAGE
        scope.launch {
            Badges.forEach { badge ->
                dao.upsert(
                    BadgeProgressEntity(
                        badgeId    = badge.id,
                        // REQUIRED FOR ROOM BADGE STORAGE — store as Int to avoid Float precision issues
                        progress   = (badge.progress * 100).toInt(),
                        unlocked   = badge.unlocked,
                        // REQUIRED FOR ROOM BADGE STORAGE — record unlock time on first earn
                        unlockedAtMillis = if (badge.unlocked) System.currentTimeMillis() else null,
                        timesEarned = if (badge.unlocked) 1 else 0
                    )
                )
            }
        }
    }
}