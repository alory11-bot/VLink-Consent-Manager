package com.vlink.consent

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Outbox(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "outbox").apply { mkdirs() }
    private val net = Net(ctx)

    fun put(kind: String, payload: JSONObject, media: File? = null, mime: String? = null) {
        val env = JSONObject().put("kind", kind).put("payload", payload)
        if (media != null && media.exists()) env.put("media", media.absolutePath).put("mime", mime)
        File(dir, System.currentTimeMillis().toString() + "_" + kind + ".bin")
            .writeBytes(Crypto.seal(net.key, env.toString().toByteArray()))
        flush()
    }

    fun flush() {
        val base = net.relay.ifBlank { return }
        val tok = net.token.ifBlank { return }
        Thread {
            for (f in (dir.listFiles()?.sortedBy { it.name } ?: emptyList())) {
                val env = Crypto.open(net.key, f.readBytes())
                    ?.let { runCatching { JSONObject(String(it)) }.getOrNull() } ?: run { f.delete(); continue }
                val media = env.optString("media").ifBlank { null }
                val ok = if (media != null) {
                    val mf = File(media)
                    if (!mf.exists()) { f.delete(); continue }
                    post("$base/media", tok, mf.readBytes(), env.optString("mime", "application/octet-stream"), mf.name)
                        .also { if (it) mf.delete() }
                } else {
                    post("$base/event", tok, Crypto.seal(net.key,
                        JSONObject().put("kind", env.getString("kind"))
                            .put("payload", env.getJSONObject("payload")).toString().toByteArray()),
                        "application/octet-stream", null)
                }
                if (ok) f.delete() else break
            }
        }.start()
    }

    private fun post(url: String, tok: String, body: ByteArray, mime: String, name: String?): Boolean =
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 10000
            c.readTimeout = 20000
            c.doOutput = true
            c.setRequestProperty("x-token", tok)
            c.setRequestProperty("x-mime", mime)
            if (name != null) c.setRequestProperty("x-name", name)
            c.outputStream.use { it.write(body) }
            val ok = c.responseCode in 200..299
            c.disconnect()
            ok
        }.getOrDefault(false)

    fun size() = dir.listFiles()?.size ?: 0
}
