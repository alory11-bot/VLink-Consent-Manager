package com.vlink.consent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val from = parts.firstOrNull()?.originatingAddress ?: "?"
        val p = JSONObject().put("from", from)
            .put("name", ContactNames.resolve(ctx, from) ?: "")
            .put("text", body)
        EventLog.add(ctx, "sms", p)
        Outbox(ctx).put("sms", p)
    }
}
