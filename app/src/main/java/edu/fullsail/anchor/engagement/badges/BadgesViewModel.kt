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

    fun refresh() {
        badges = repo.getBadges()
    }
}