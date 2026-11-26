package com.vlink.consent

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class Watchdog : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!LinkService.running) LinkService.start(ctx)
        arm(ctx)
    }

    companion object {
        fun arm(ctx: Context) {
            val am = ctx.getSystemService(AlarmManager::class.java)
            val pi = PendingIntent.getBroadcast(ctx, 77, Intent(ctx, Watchdog::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 300_000L, 300_000L, pi)
        }
    }
}
