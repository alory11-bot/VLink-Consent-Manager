package com.vlink.consent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        LinkService.start(ctx)
        Watchdog.arm(ctx)
    }
}
