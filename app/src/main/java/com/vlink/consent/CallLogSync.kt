package com.vlink.consent

import android.content.Context
import android.provider.CallLog
import org.json.JSONObject

object CallLogSync {
    private val TYPES = mapOf(
        CallLog.Calls.INCOMING_TYPE to "incoming",
        CallLog.Calls.OUTGOING_TYPE to "outgoing",
        CallLog.Calls.MISSED_TYPE to "missed",
        CallLog.Calls.REJECTED_TYPE to "rejected"
    )

    fun push(ctx: Context, limit: Int = 50): Int {
        var n = 0
        val cols = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE)
        ctx.contentResolver.query(CallLog.Calls.CONTENT_URI, cols, null, null,
            CallLog.Calls.DATE + " DESC")?.use { c ->
            while (c.moveToNext() && n < limit) {
                val num = c.getString(0) ?: continue
                val p = JSONObject()
                    .put("number", num)
                    .put("name", ContactNames.resolve(ctx, num) ?: "")
                    .put("type", TYPES[c.getInt(1)] ?: "other")
                    .put("duration", c.getLong(2))
                    .put("at", c.getLong(3))
                EventLog.add(ctx, "call", p)
                Outbox(ctx).put("call", p)
                n++
            }
        }
        return n
    }
}
