package edu.fullsail.anchor.engagement.badges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class BadgesViewModel (
    private val repo: BadgeRepository = InMemoryBadgeRepository()
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