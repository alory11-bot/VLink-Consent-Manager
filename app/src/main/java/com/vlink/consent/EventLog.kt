package com.vlink.consent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject

object EventLog {
    private const val CAP = 500

    fun channel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("vlink_evt", "رویدادها", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun add(ctx: Context, kind: String, payload: JSONObject) {
        val store = SecureStore(ctx)
        val arr = runCatching { JSONArray(store.get("events") ?: "[]") }.getOrDefault(JSONArray())
        arr.put(JSONObject().put("at", System.currentTimeMillis()).put("kind", kind).put("body", payload.toString()))
        val out = JSONArray()
        val from = (arr.length() - CAP).coerceAtLeast(0)
        for (i in from until arr.length()) out.put(arr.getJSONObject(i))
        store.put("events", out.toString())
        notify(ctx, kind, payload)
    }

    fun all(ctx: Context): List<JSONObject> = runCatching {
        val arr = JSONArray(SecureStore(ctx).get("events") ?: "[]")
        (0 until arr.length()).map { arr.getJSONObject(it) }.reversed()
    }.getOrDefault(emptyList())

    fun clear(ctx: Context) = SecureStore(ctx).remove("events")

    private fun notify(ctx: Context, kind: String, p: JSONObject) {
        val title = when (kind) {
            "sms" -> "پیامک از " + p.optString("from")
            "call" -> "تماس " + p.optString("number")
            "notif" -> p.optString("app") + ": " + p.optString("title")
            "location" -> "موقعیت جدید"
            "photo" -> "عکس جدید"
            "video" -> "ویدیو جدید"
            "audio" -> "صدا جدید"
            "keylog" -> "کیلاگ"
            "battery" -> "وضعیت دستگاه"
            "wifi" -> "شبکه"
            "apps" -> "فهرست برنامه‌ها"
            "contact" -> "مخاطب"
            else -> kind
        }
        ctx.getSystemService(NotificationManager::class.java).notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(ctx, "vlink_evt")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(p.optString("text").ifBlank { p.optString("body") })
                .setAutoCancel(true).build()
        )
    }
}
