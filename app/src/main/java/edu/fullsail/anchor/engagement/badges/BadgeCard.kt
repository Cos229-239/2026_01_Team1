package edu.fullsail.anchor.engagement.badges

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BadgeCard(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Left Side: Placeholder to swap out later
        Text(
            text = if (badge.unlocked) "..." else ",,,",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 12.dp)
        )
        //Middle: Text Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = badge.description,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { badge.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text =  "${(badge.progress.coerceIn(0f, 1f) * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium
        )
    }
}