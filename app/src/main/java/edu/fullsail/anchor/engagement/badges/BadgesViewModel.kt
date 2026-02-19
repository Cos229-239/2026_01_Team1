package edu.fullsail.anchor.engagement.badges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// REQUIRED FOR ROOM BADGE STORAGE
// RoomBadgeRepository is in the same package (edu.fullsail.anchor.engagement.badges)
// so no import is needed — Kotlin finds it automatically within the same package.

// MODIFIED FOR PERSISTENCE
// Default parameter changed from InMemoryBadgeRepository to RoomBadgeRepository.
// Public API (badges, refreshBadges(), saveBadges()) is IDENTICAL — no UI changes needed.
// The factory at the bottom is required because the constructor now takes a parameter
// that must be built with a Context (via AnchorDatabase.getInstance).
class BadgesViewModel (
    // MODIFIED FOR PERSISTENCE — RoomBadgeRepository is the new default storage backend
    // InMemoryBadgeRepository is kept in the codebase but no longer used here.
    private val repo: BadgeRepository
) : ViewModel() {
    var badges by mutableStateOf(repo.getBadges())
        private set
    init {
        refreshBadges()
    }
    fun refreshBadges() {
        badges = repo.getBadges()
    }
    fun saveBadges(updated: List<Badge>){
        repo.saveBadges(updated)
        badges = updated
    }
}

// REQUIRED FOR ROOM BADGE STORAGE
/**
 * Factory required to construct BadgesViewModel with a BadgeRepository parameter.
 * Pass this factory wherever viewModel() is called for BadgesViewModel.
 *
 * Usage in AppNavigation:
 *   val badgeRepo = RoomBadgeRepository(AnchorDatabase.getInstance(context).badgeProgressDao())
 *   val badgesViewModelFactory = remember { BadgesViewModelFactory(badgeRepo) }
 *   val badgesViewModel: BadgesViewModel = viewModel(factory = badgesViewModelFactory)
 */
class BadgesViewModelFactory(
    // REQUIRED FOR ROOM BADGE STORAGE
    private val repo: BadgeRepository
) : ViewModelProvider.Factory {
    // REQUIRED FOR ROOM BADGE STORAGE
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BadgesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BadgesViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}