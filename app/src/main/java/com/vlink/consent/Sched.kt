package com.vlink.consent

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SchedReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        CaptureService.run(ctx, CaptureService.A_AUDIO, intent.getIntExtra("sec", 30))
    }
}

object Sched {
    fun at(ctx: Context, atMillis: Long, seconds: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(ctx, (atMillis % 100000).toInt(),
            Intent(ctx, SchedReceiver::class.java).putExtra("sec", seconds),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }

    fun every(ctx: Context, minutes: Int, seconds: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(ctx, 501,
            Intent(ctx, SchedReceiver::class.java).putExtra("sec", seconds),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60_000L, minutes * 60_000L, pi)
    }
}
