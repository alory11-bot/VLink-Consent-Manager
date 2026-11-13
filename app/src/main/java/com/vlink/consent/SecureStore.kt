package com.vlink.consent

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted local preferences using an AES-GCM key protected by Android Keystore. */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("vlink_secure_v1", Context.MODE_PRIVATE)
    private val alias = "vlink_local_aes_v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? = runCatching {
        val encoded = prefs.getString(name, null) ?: return null
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        require(raw.size > 12) { "Invalid encrypted value" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
    }.getOrNull()

    fun remove(name: String) { prefs.edit().remove(name).apply() }
}
