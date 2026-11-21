package com.vlink.consent

import android.content.Context

class Net(ctx: Context) {
    private val store = SecureStore(ctx)

    var relay: String
        get() = store.get("relay") ?: ""
        set(v) { store.put("relay", v) }

    var token: String
        get() = store.get("token") ?: ""
        set(v) { store.put("token", v) }

    var deviceId: String
        get() = store.get("device_id") ?: ""
        set(v) { store.put("device_id", v) }

    val key: ByteArray
        get() {
            val saved = store.get("link_key") ?: ""
            if (saved.isNotBlank()) return Crypto.unb64(saved)
            val k = Crypto.newKey()
            store.put("link_key", Crypto.b64(k))
            return k
        }
}
