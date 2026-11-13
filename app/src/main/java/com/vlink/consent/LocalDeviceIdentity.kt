package com.vlink.consent

import android.content.Context
import java.util.UUID

/**
 * Stable, per-install identifier.
 *
 * This identifier is for local device continuity and display only. It is not a
 * credential, public key, or proof of peer identity. A peer must still pass
 * the Nearby authentication-digit confirmation on every connection.
 */
data class LocalDeviceIdentity(
    val id: String,
    val createdAtMillis: Long
) {
    val shortId: String get() = id.replace("-", "").take(8).uppercase()

    companion object {
        private const val VERSION = "1"

        fun create(nowMillis: Long): LocalDeviceIdentity {
            require(nowMillis > 0L)
            return LocalDeviceIdentity(UUID.randomUUID().toString(), nowMillis)
        }

        fun decode(encoded: String): LocalDeviceIdentity? = runCatching {
            val parts = encoded.split('|')
            require(parts.size == 3 && parts[0] == VERSION)
            val uuid = UUID.fromString(parts[1])
            require(uuid.toString() == parts[1])
            val createdAt = parts[2].toLong()
            require(createdAt > 0L)
            LocalDeviceIdentity(uuid.toString(), createdAt)
        }.getOrNull()

        fun encode(identity: LocalDeviceIdentity): String {
            require(UUID.fromString(identity.id).toString() == identity.id)
            require(identity.createdAtMillis > 0L)
            return "$VERSION|${identity.id}|${identity.createdAtMillis}"
        }
    }
}

/** Stores the install identity inside SecureStore (AES-GCM / Android Keystore). */
class LocalDeviceIdentityStore(context: Context) {
    private val secureStore = SecureStore(context.applicationContext)

    @Synchronized
    fun getOrCreate(nowMillis: Long = System.currentTimeMillis()): LocalDeviceIdentity {
        LocalDeviceIdentity.decode(secureStore.get(STORAGE_KEY).orEmpty())?.let { return it }
        val identity = LocalDeviceIdentity.create(nowMillis)
        secureStore.put(STORAGE_KEY, LocalDeviceIdentity.encode(identity))
        return identity
    }

    companion object {
        private const val STORAGE_KEY = "local_device_identity_v1"
    }
}
