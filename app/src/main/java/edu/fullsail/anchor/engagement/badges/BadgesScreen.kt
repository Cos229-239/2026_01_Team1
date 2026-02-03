package edu.fullsail.anchor.engagement.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.items



@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun BadgesScreen(
    badgesViewModel: BadgesViewModel = viewModel()
) {
    val badges = badgesViewModel.badges

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Badges") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() +12.dp,
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = badges,
                key = { it.id }
            ) { badge ->
                BadgeCard(badge = badge)
            }
        }
    }
}

