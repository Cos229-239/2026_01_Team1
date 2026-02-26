package edu.fullsail.anchor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Base notification scaffolding for future reminders
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

// Sends notification when new task is created
fun sendTaskCreatedNotification(context: Context, taskTitle: String) {
    val builder = NotificationCompat.Builder(context, ANCHOR_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_input_add)
        .setContentTitle("New Task Created")
        .setContentText("You added: $taskTitle")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    val manager = NotificationManagerCompat.from(context)

    if (manager.areNotificationsEnabled()) {
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}