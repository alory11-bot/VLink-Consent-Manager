package com.vlink.consent

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat

object Fg {
    fun build(ctx: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(ctx, "vlink_evt")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title).setContentText(text)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
}
