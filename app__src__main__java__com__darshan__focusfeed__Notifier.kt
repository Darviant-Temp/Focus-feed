package com.darshan.focusfeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifier {
    private val lines = listOf(
        "15 focused minutes. One card at a time.",
        "Open Focus Feed and pick up where you left off.",
        "Your streak is waiting. Read one batch now.",
        "Small session, big difference. Start now."
    )

    fun show(ctx: Context, title: String, text: String?, id: Int) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("nudge", "Study nudges", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, "nudge")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text ?: lines.random())
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }
}
