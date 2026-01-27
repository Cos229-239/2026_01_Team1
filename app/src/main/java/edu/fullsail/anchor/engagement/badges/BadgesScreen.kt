package edu.fullsail.anchor.engagement.badges

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun BadgesScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Badges") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Badges",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

