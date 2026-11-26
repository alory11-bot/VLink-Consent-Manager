package com.vlink.consent

import android.telecom.Call
import android.telecom.CallScreeningService
import org.json.JSONObject

class CallScreen : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart ?: "?"
        val p = JSONObject().put("number", number)
            .put("name", ContactNames.resolve(this, number) ?: "")
        EventLog.add(this, "call", p)
        Outbox(this).put("call", p)
        respondToCall(details, CallResponse.Builder().build())
    }
}
