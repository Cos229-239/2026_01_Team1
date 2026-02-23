package edu.fullsail.anchor.engagement.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items


/**
 * Screen responsible for displaying all user badges.
 *
 * Responsibilities:
 * - Observe badge state from the ViewModel
 * - Display badges in a scrollable list
 * - Apply proper scaffold padding
 *
 * This composable does NOT:
 * - Handle business logic
 * - Modify badge state
 * - Evaluate badge rules
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    badgesViewModel: BadgesViewModel
) {
    /**
     * Reads the current badge list from the ViewModel.
     *
     * Ideally, this should come from a State or StateFlow
     * to ensure proper recomposition when badge data changes.
     */
    val badges = badgesViewModel.badges
    /**
     * Scaffold provides layout structure and handles
     * system insets (status bar, navigation bar).
     */
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Applies screen padding while respecting Scaffold insets
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() +12.dp,
                bottom = innerPadding.calculateBottomPadding()
            ),
            // Adds vertical spacing between badge cards
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /**
             * items() builds each badge entry.
             *
             * key = { it.id } ensures stable item identity,
             * improving performance and preventing unnecessary recomposition.
             */
            items(
                items = badges,
                key = { it.id }
            ) { badge ->
                // Reusable UI component for displaying individual badge
                BadgeCard(badge = badge)
            }
        }
    }
}

