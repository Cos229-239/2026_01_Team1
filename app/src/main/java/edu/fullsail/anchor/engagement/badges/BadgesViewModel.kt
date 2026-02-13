package edu.fullsail.anchor.engagement.badges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * ViewModel responsible for providing badge state to the UI layer.
 *
 * Responsibilities:
 * - Expose a Compose-friendly observable list of badges
 * - Load badges from a repository
 * - Save updated badges back to the repository
 *
 * The ViewModel does NOT evaluate badge rules itself; that should happen
 * in the badge engine/rule layer, then the result is saved via this ViewModel.
 */
class BadgesViewModel (
    /**
     * Repository dependency for retrieving/persisting badges.
     *
     * Default is an in-memory implementation for development/testing.
     * Later you can swap to Room/DataStore without changing UI code.
     */
    private val repo: BadgeRepository = InMemoryBadgeRepository()
    ) : ViewModel() {
    /**
     * Current badge list exposed to Compose.
     *
     * mutableStateOf makes this observable:
     * when `badges` is reassigned, Composables reading it will recompose.
     *
     * `private set` prevents UI classes from accidentally mutating state.
     */
    var badges by mutableStateOf(repo.getBadges())
        private set
    /**
     * init runs once when the ViewModel is created.
     * We refresh to ensure the latest repository state is loaded.
     */
    init {
        refreshBadges()
    }
    /**
     * Reloads badge list from the repository.
     *
     * Useful when:
     * - Returning to the screen
     * - Another feature updated badges
     * - You want to force UI refresh from storage
     */
    fun refreshBadges() {
        badges = repo.getBadges()
    }
    /**
     * Persists updated badges and immediately updates UI state.
     *
     * This keeps the UI in sync after:
     * - Progress changes
     * - Unlock events
     */
    fun saveBadges(updated: List<Badge>){
        repo.saveBadges(updated)
        badges = updated
    }
}