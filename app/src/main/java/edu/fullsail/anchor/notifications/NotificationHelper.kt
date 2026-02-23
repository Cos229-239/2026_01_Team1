package edu.fullsail.anchor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// Base notification scaffolding for future reminders
// Actual sending/scheduling will be added later

const val ANCHOR_CHANNEL_ID = "anchor_notifications"
const val ANCHOR_CHANNEL_NAME = "Anchor Reminders"

fun createAnchorNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ANCHOR_CHANNEL_ID,
            ANCHOR_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}