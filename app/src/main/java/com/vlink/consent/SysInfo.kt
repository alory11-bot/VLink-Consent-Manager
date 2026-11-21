package com.vlink.consent

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import org.json.JSONArray
import org.json.JSONObject

object SysInfo {
    fun battery(ctx: Context) {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (scale > 0) level * 100 / scale else -1
        val charging = (i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            ?: -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val pm = ctx.getSystemService(PowerManager::class.java)
        val p = JSONObject()
            .put("battery", pct)
            .put("charging", charging)
            .put("screen_on", pm.isInteractive)
            .put("model", android.os.Build.MODEL)
            .put("sdk", android.os.Build.VERSION.SDK_INT)
        EventLog.add(ctx, "battery", p)
        Outbox(ctx).put("battery", p)
    }

    @Suppress("DEPRECATION")
    fun wifi(ctx: Context) {
        val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
        val info = wm.connectionInfo
        val ip = info?.ipAddress?.let {
            String.format("%d.%d.%d.%d", it and 0xff, it shr 8 and 0xff, it shr 16 and 0xff, it shr 24 and 0xff)
        } ?: ""
        val p = JSONObject().put("ssid", info?.ssid ?: "").put("ip", ip)
        EventLog.add(ctx, "wifi", p)
        Outbox(ctx).put("wifi", p)
    }

    fun apps(ctx: Context) {
        val list = ctx.packageManager.getInstalledApplications(0)
        val arr = JSONArray()
        list.take(400).forEach { arr.put(it.packageName) }
        val p = JSONObject().put("count", list.size).put("list", arr)
        EventLog.add(ctx, "apps", JSONObject().put("count", list.size))
        Outbox(ctx).put("apps", p)
    }
}
