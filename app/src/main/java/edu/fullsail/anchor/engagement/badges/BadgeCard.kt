package edu.fullsail.anchor.engagement.badges

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BadgeCard(
    badge: Badge,
    modifier: Modifier = Modifier
) {Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Left Side: Placeholder to swap out later
        Image(
            painter = painterResource(id = badge.iconRes),
            contentDescription = badge.title,
            modifier = Modifier
                .size(96.dp)
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .alpha(if (badge.unlocked) 1f else 0.4f)
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
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { badge.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${(badge.progress.coerceIn(0f, 1f) * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium
        )
    }
}
}